package com.unaj.senderos.presentation.formulario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.EdicionRecorrido
import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.SenderoInvalidoException
import com.unaj.senderos.domain.repository.AlmacenFotos
import com.unaj.senderos.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.presentation.comun.formatearDecimalEditable
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "FORMULARIO_SENDERO"
private const val MENSAJE_ERROR_POR_DEFECTO = "Ocurrió un error inesperado"
private const val MENSAJE_SENDERO_NO_ENCONTRADO = "No se encontró el sendero"
private const val MENSAJE_REVISAR_CAMPOS = "Revisá los campos marcados"
private const val MENSAJE_ETAPA_INVALIDA = "Completá el nombre y los km de la etapa"
private const val MENSAJE_FOTO_INVALIDA = "No se pudo usar esa foto"

class FormularioSenderoViewModel(
    private val idSendero: String?,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val guardarSenderoUseCase: GuardarSenderoUseCase,
    private val almacenFotos: AlmacenFotos
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(
        FormularioSenderoEstadoVisual(esEdicion = idSendero != null, estaCargando = idSendero != null)
    )
    val estadoVisual: StateFlow<FormularioSenderoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<FormularioSenderoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    // Sendero original en edición, para conservar sus datos no editables al guardar
    private var senderoOriginal: Sendero? = null

    init {
        if (idSendero != null) cargarSendero(idSendero)
    }

    // Procesar cada evento del formulario de sendero
    fun procesarEvento(evento: FormularioSenderoEvento) {
        when (evento) {
            is FormularioSenderoEvento.AlCambiarTexto -> cambiarTexto(evento.campo, evento.texto)
            is FormularioSenderoEvento.AlSeleccionarDificultad -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(dificultad = evento.dificultad, hayCambiosSinGuardar = true)
                }
            }
            is FormularioSenderoEvento.AlSeleccionarTipoRecorrido -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(tipoRecorrido = evento.tipoRecorrido, hayCambiosSinGuardar = true)
                }
            }
            is FormularioSenderoEvento.AlAgregarRecomendacion -> agregarRecomendacion()
            is FormularioSenderoEvento.AlQuitarRecomendacion -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(
                        recomendaciones = estadoActual.recomendaciones.filterIndexed { indice, _ -> indice != evento.indice },
                        hayCambiosSinGuardar = true
                    )
                }
            }
            is FormularioSenderoEvento.AlAgregarServicio -> agregarServicio()
            is FormularioSenderoEvento.AlQuitarServicio -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(
                        servicios = estadoActual.servicios.filterIndexed { indice, _ -> indice != evento.indice },
                        hayCambiosSinGuardar = true
                    )
                }
            }
            is FormularioSenderoEvento.AlAgregarEtapa -> agregarEtapa()
            is FormularioSenderoEvento.AlQuitarEtapa -> quitarEtapa(evento.indice)
            is FormularioSenderoEvento.AlGuardar -> guardar()
            is FormularioSenderoEvento.AlVolver -> volver()
            is FormularioSenderoEvento.AlConfirmarDescartar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoDescartarVisible = false) }
                emitirEfecto(FormularioSenderoEfecto.VolverAtras)
            }
            is FormularioSenderoEvento.AlAbrirEditorMapa -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esEditorMapaVisible = true) }
            }
            is FormularioSenderoEvento.AlCerrarEditorMapa -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(esEditorMapaVisible = false, indicePuntoSeleccionado = null)
                }
            }
            is FormularioSenderoEvento.AlAgregarPunto -> editarRecorrido { it.agregar(evento.latitud, evento.longitud) }
            is FormularioSenderoEvento.AlMoverPunto -> {
                editarRecorrido { it.mover(evento.indice, evento.latitud, evento.longitud) }
            }
            is FormularioSenderoEvento.AlSeleccionarPunto -> seleccionarPunto(evento.indice)
            is FormularioSenderoEvento.AlEliminarPunto -> editarRecorrido { it.eliminar(evento.indice) }
            is FormularioSenderoEvento.AlInsertarPuntoAntes -> editarRecorrido { it.insertarAntes(evento.indice) }
            is FormularioSenderoEvento.AlCambiarTipoPunto -> {
                editarRecorrido { it.reemplazar(evento.indice) { punto -> punto.copy(tipo = evento.tipo) } }
            }
            is FormularioSenderoEvento.AlCambiarNombrePunto -> {
                editarRecorrido { it.reemplazar(evento.indice) { punto -> punto.copy(nombre = evento.nombre) } }
            }
            is FormularioSenderoEvento.AlInvertirSentido -> editarRecorrido { it.invertir() }
            is FormularioSenderoEvento.AlUsarDistanciaDelMapa -> usarDistanciaDelMapa()
            is FormularioSenderoEvento.AlSeleccionarFoto -> seleccionarFoto(evento.uri)
            is FormularioSenderoEvento.AlQuitarFoto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(foto = "", hayCambiosSinGuardar = true) }
            }
            is FormularioSenderoEvento.AlCancelarDescartar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoDescartarVisible = false) }
            }
        }
    }

    // Cargar el sendero a editar y volcar sus datos en el estado visual
    private fun cargarSendero(idSenderoEditado: String) {
        viewModelScope.launch {
            val resultado = observarDetalleSenderoUseCase(idSenderoEditado)
            val falloInicial = resultado.exceptionOrNull()
            if (falloInicial != null) {
                Log.e(TAG, "No se pudo observar el sendero", falloInicial)
                mostrarErrorDeCarga(falloInicial.message)
                return@launch
            }
            val flujoSendero = resultado.getOrNull() ?: return@launch
            val senderoEncontrado = try {
                flujoSendero.first()
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "Error al cargar el sendero", error)
                mostrarErrorDeCarga(error.message)
                return@launch
            }
            if (senderoEncontrado == null) {
                mostrarErrorDeCarga(MENSAJE_SENDERO_NO_ENCONTRADO)
                return@launch
            }
            senderoOriginal = senderoEncontrado
            _estadoVisual.update { estadoActual ->
                estadoActual.copy(
                    estaCargando = false,
                    mensajeError = null,
                    nombre = senderoEncontrado.nombre,
                    region = senderoEncontrado.region,
                    descripcion = senderoEncontrado.descripcion,
                    distancia = formatearDecimalEditable(senderoEncontrado.distanciaKilometros),
                    desnivel = senderoEncontrado.desnivelMetros.toString(),
                    alturaMaxima = senderoEncontrado.alturaMaximaMetros.toString(),
                    alturaMinima = senderoEncontrado.alturaMinimaMetros.toString(),
                    duracionEstimada = senderoEncontrado.duracionEstimada,
                    puntoInicio = senderoEncontrado.puntoInicio,
                    mejorEpoca = senderoEncontrado.mejorEpoca,
                    dificultad = senderoEncontrado.dificultad,
                    tipoRecorrido = senderoEncontrado.tipoRecorrido,
                    recomendaciones = senderoEncontrado.recomendaciones,
                    servicios = senderoEncontrado.servicios,
                    etapas = senderoEncontrado.etapas,
                    recorrido = senderoEncontrado.recorrido,
                    foto = senderoEncontrado.foto,
                    hayCambiosSinGuardar = false
                )
            }
        }
    }

    // Mostrar el error ocurrido al cargar el sendero a editar
    private fun mostrarErrorDeCarga(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaCargando = false, mensajeError = mensaje) }
    }

    // Editar el recorrido con la operación dada; una operación que no cambia nada (índice fuera de rango, invertir un solo punto) no cuenta como cambio
    private fun editarRecorrido(operacion: (EdicionRecorrido) -> EdicionRecorrido) {
        _estadoVisual.update { estado ->
            val actual = EdicionRecorrido(estado.recorrido, estado.indicePuntoSeleccionado)
            val editado = operacion(actual)
            if (editado == actual) {
                estado
            } else {
                estado.copy(recorrido = editado.puntos, indicePuntoSeleccionado = editado.seleccion, hayCambiosSinGuardar = true)
            }
        }
    }

    // Seleccionar un punto del recorrido para editarlo, o deseleccionarlo
    private fun seleccionarPunto(indice: Int?) {
        if (indice != null && indice !in _estadoVisual.value.recorrido.indices) return
        _estadoVisual.update { estado -> estado.copy(indicePuntoSeleccionado = indice) }
    }

    // Copiar la distancia calculada del recorrido al campo de distancia
    private fun usarDistanciaDelMapa() {
        val distanciaRedondeada = Math.round(_estadoVisual.value.distanciaCalculadaKilometros * 10) / 10.0
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(
                distancia = formatearDecimalEditable(distanciaRedondeada),
                camposConError = estadoActual.camposConError - CampoSendero.DISTANCIA,
                hayCambiosSinGuardar = true
            )
        }
    }

    // Reemplazar el texto de un campo del formulario y limpiar su error
    private fun cambiarTexto(campo: CampoFormulario, texto: String) {
        _estadoVisual.update { estadoActual ->
            val estadoConTexto = when (campo) {
                CampoFormulario.NOMBRE -> estadoActual.copy(nombre = texto)
                CampoFormulario.REGION -> estadoActual.copy(region = texto)
                CampoFormulario.DESCRIPCION -> estadoActual.copy(descripcion = texto)
                CampoFormulario.DISTANCIA -> estadoActual.copy(distancia = texto)
                CampoFormulario.DESNIVEL -> estadoActual.copy(desnivel = texto)
                CampoFormulario.ALTURA_MAXIMA -> estadoActual.copy(alturaMaxima = texto)
                CampoFormulario.ALTURA_MINIMA -> estadoActual.copy(alturaMinima = texto)
                CampoFormulario.DURACION -> estadoActual.copy(duracionEstimada = texto)
                CampoFormulario.PUNTO_INICIO -> estadoActual.copy(puntoInicio = texto)
                CampoFormulario.MEJOR_EPOCA -> estadoActual.copy(mejorEpoca = texto)
                CampoFormulario.NUEVA_RECOMENDACION -> estadoActual.copy(textoNuevaRecomendacion = texto)
                CampoFormulario.NUEVO_SERVICIO -> estadoActual.copy(textoNuevoServicio = texto)
                CampoFormulario.NOMBRE_NUEVA_ETAPA -> estadoActual.copy(nombreNuevaEtapa = texto)
                CampoFormulario.DISTANCIA_NUEVA_ETAPA -> estadoActual.copy(distanciaNuevaEtapa = texto)
            }
            val campoDelSendero = obtenerCampoDelSendero(campo)
            val camposRestantes = if (campoDelSendero != null) {
                estadoConTexto.camposConError - campoDelSendero
            } else {
                estadoConTexto.camposConError
            }
            val modificaDatos = campo != CampoFormulario.NUEVA_RECOMENDACION &&
                campo != CampoFormulario.NUEVO_SERVICIO &&
                campo != CampoFormulario.NOMBRE_NUEVA_ETAPA &&
                campo != CampoFormulario.DISTANCIA_NUEVA_ETAPA
            estadoConTexto.copy(
                camposConError = camposRestantes,
                hayCambiosSinGuardar = estadoConTexto.hayCambiosSinGuardar || modificaDatos
            )
        }
    }

    // Obtener el campo de validación del sendero que corresponde a un campo del formulario
    private fun obtenerCampoDelSendero(campo: CampoFormulario): CampoSendero? {
        return when (campo) {
            CampoFormulario.NOMBRE -> CampoSendero.NOMBRE
            CampoFormulario.REGION -> CampoSendero.REGION
            CampoFormulario.DESCRIPCION -> CampoSendero.DESCRIPCION
            CampoFormulario.DISTANCIA -> CampoSendero.DISTANCIA
            CampoFormulario.DESNIVEL -> CampoSendero.DESNIVEL
            CampoFormulario.ALTURA_MAXIMA -> CampoSendero.ALTURA_MAXIMA
            CampoFormulario.ALTURA_MINIMA -> CampoSendero.ALTURA_MINIMA
            else -> null
        }
    }

    // Agregar la recomendación escrita a la lista, si no está vacía
    private fun agregarRecomendacion() {
        val texto = _estadoVisual.value.textoNuevaRecomendacion.trim()
        if (texto.isEmpty()) return
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(
                recomendaciones = estadoActual.recomendaciones + texto,
                textoNuevaRecomendacion = "",
                hayCambiosSinGuardar = true
            )
        }
    }

    // Agregar el servicio escrito a la lista, si no está vacío
    private fun agregarServicio() {
        val texto = _estadoVisual.value.textoNuevoServicio.trim()
        if (texto.isEmpty()) return
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(
                servicios = estadoActual.servicios + texto,
                textoNuevoServicio = "",
                hayCambiosSinGuardar = true
            )
        }
    }

    // Agregar la etapa escrita a la lista si su nombre y su distancia son válidos
    private fun agregarEtapa() {
        val estadoActual = _estadoVisual.value
        val nombre = estadoActual.nombreNuevaEtapa.trim()
        val distancia = interpretarDecimal(estadoActual.distanciaNuevaEtapa)
        if (nombre.isEmpty() || distancia == null || distancia <= 0.0) {
            emitirEfecto(FormularioSenderoEfecto.MostrarAviso(MENSAJE_ETAPA_INVALIDA))
            return
        }
        _estadoVisual.update { estadoPrevio ->
            estadoPrevio.copy(
                etapas = estadoPrevio.etapas + Etapa(
                    numero = estadoPrevio.etapas.size + 1,
                    nombre = nombre,
                    distanciaKilometros = distancia
                ),
                nombreNuevaEtapa = "",
                distanciaNuevaEtapa = "",
                hayCambiosSinGuardar = true
            )
        }
    }

    // Quitar una etapa de la lista y renumerar las restantes
    private fun quitarEtapa(indice: Int) {
        _estadoVisual.update { estadoActual ->
            val etapasRestantes = estadoActual.etapas
                .filterIndexed { posicion, _ -> posicion != indice }
                .mapIndexed { posicion, etapa -> etapa.copy(numero = posicion + 1) }
            estadoActual.copy(etapas = etapasRestantes, hayCambiosSinGuardar = true)
        }
    }

    // Interpretar un texto como número decimal, o nulo si no lo es
    private fun interpretarDecimal(texto: String): Double? {
        val valor = texto.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return valor.takeIf { numero -> numero.isFinite() }
    }

    // Validar y guardar el sendero, y avisar el resultado a la pantalla
    private fun guardar() {
        val estadoActual = _estadoVisual.value
        if (estadoActual.estaGuardando || estadoActual.estaCargando || estadoActual.mensajeError != null) return
        if (estadoActual.estaProcesandoFoto) return

        val distancia = interpretarDecimal(estadoActual.distancia)
        val desnivel = estadoActual.desnivel.trim().toIntOrNull()
        val alturaMaxima = estadoActual.alturaMaxima.trim().toIntOrNull()
        val alturaMinima = estadoActual.alturaMinima.trim().toIntOrNull()

        if (distancia == null || desnivel == null || alturaMaxima == null || alturaMinima == null) {
            val camposNumericosInvalidos = buildSet {
                if (distancia == null) add(CampoSendero.DISTANCIA)
                if (desnivel == null) add(CampoSendero.DESNIVEL)
                if (alturaMaxima == null) add(CampoSendero.ALTURA_MAXIMA)
                if (alturaMinima == null) add(CampoSendero.ALTURA_MINIMA)
            }
            _estadoVisual.update { estadoPrevio -> estadoPrevio.copy(camposConError = camposNumericosInvalidos) }
            emitirEfecto(FormularioSenderoEfecto.MostrarAviso(MENSAJE_REVISAR_CAMPOS))
            return
        }

        val senderoBase = senderoOriginal ?: Sendero(
            id = "",
            idUsuario = "",
            nombre = "",
            region = "",
            distanciaKilometros = 0.0,
            dificultad = estadoActual.dificultad
        )
        val senderoArmado = senderoBase.copy(
            nombre = estadoActual.nombre,
            region = estadoActual.region,
            descripcion = estadoActual.descripcion,
            distanciaKilometros = distancia,
            desnivelMetros = desnivel,
            alturaMaximaMetros = alturaMaxima,
            alturaMinimaMetros = alturaMinima,
            duracionEstimada = estadoActual.duracionEstimada,
            puntoInicio = estadoActual.puntoInicio,
            mejorEpoca = estadoActual.mejorEpoca,
            dificultad = estadoActual.dificultad,
            tipoRecorrido = estadoActual.tipoRecorrido,
            recomendaciones = estadoActual.recomendaciones,
            servicios = estadoActual.servicios,
            etapas = estadoActual.etapas,
            recorrido = estadoActual.recorrido,
            foto = estadoActual.foto
        )

        _estadoVisual.update { estadoPrevio -> estadoPrevio.copy(estaGuardando = true, camposConError = emptySet()) }
        viewModelScope.launch {
            guardarSenderoUseCase(senderoArmado).fold(
                onSuccess = { senderoGuardado ->
                    _estadoVisual.update { estadoPrevio ->
                        estadoPrevio.copy(estaGuardando = false, hayCambiosSinGuardar = false)
                    }
                    canalEfectos.send(FormularioSenderoEfecto.SenderoGuardado(senderoGuardado.id))
                },
                onFailure = { error -> manejarFalloDeGuardado(error) }
            )
        }
    }

    // Reflejar en el estado visual el fallo al guardar, marcando los campos con error
    private suspend fun manejarFalloDeGuardado(error: Throwable) {
        if (error is CancellationException) throw error
        if (error is SenderoInvalidoException) {
            _estadoVisual.update { estadoPrevio ->
                estadoPrevio.copy(estaGuardando = false, camposConError = error.camposInvalidos)
            }
            canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(MENSAJE_REVISAR_CAMPOS))
        } else {
            Log.e(TAG, "Error al guardar el sendero", error)
            _estadoVisual.update { estadoPrevio -> estadoPrevio.copy(estaGuardando = false) }
            val mensaje = error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
            canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(mensaje))
        }
    }

    // Copiar la foto elegida al almacenamiento de la app para que siga disponible aunque el usuario la borre de la galería
    private fun seleccionarFoto(uri: String) {
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoFoto = true) }
        viewModelScope.launch {
            almacenFotos.guardarCopia(uri).fold(
                onSuccess = { rutaLocal ->
                    _estadoVisual.update { estadoActual ->
                        estadoActual.copy(foto = rutaLocal, estaProcesandoFoto = false, hayCambiosSinGuardar = true)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo copiar la foto elegida", error)
                    _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoFoto = false) }
                    canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(MENSAJE_FOTO_INVALIDA))
                }
            )
        }
    }

    // Volver atrás, pidiendo confirmación si hay cambios sin guardar
    private fun volver() {
        if (_estadoVisual.value.hayCambiosSinGuardar) {
            _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoDescartarVisible = true) }
        } else {
            emitirEfecto(FormularioSenderoEfecto.VolverAtras)
        }
    }

    // Emitir un efecto único hacia la pantalla
    private fun emitirEfecto(efecto: FormularioSenderoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
