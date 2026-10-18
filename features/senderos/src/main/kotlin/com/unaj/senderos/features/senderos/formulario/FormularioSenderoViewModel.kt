package com.unaj.senderos.features.senderos.formulario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.recorrido.EdicionRecorrido
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoInvalidoException
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.ui.comun.formatearDecimalEditable
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

private const val TAG = "FORMULARIO_SENDERO"
private const val MENSAJE_ERROR_POR_DEFECTO = "Ocurrió un error inesperado"
private const val MENSAJE_SENDERO_NO_ENCONTRADO = "No se encontró el sendero"
private const val MENSAJE_REVISAR_CAMPOS = "Revisá los campos marcados"
private const val MENSAJE_ETAPA_INVALIDA = "Completá el nombre y los km de la etapa"
private const val MENSAJE_FOTO_INVALIDA = "No se pudo usar esa foto"
private const val MENSAJE_FOTO_PUNTO_INVALIDA = "No se pudo guardar la foto del punto"

@HiltViewModel(assistedFactory = FormularioSenderoViewModel.Fabrica::class)
class FormularioSenderoViewModel @AssistedInject constructor(
    @Assisted private val idSendero: String?,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val guardarSenderoUseCase: GuardarSenderoUseCase,
    private val almacenFotos: AlmacenFotos
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(idSendero: String?): FormularioSenderoViewModel
    }

    private val _estadoVisual = MutableStateFlow(
        FormularioSenderoEstadoVisual(esEdicion = idSendero != null, estaCargando = idSendero != null)
    )
    val estadoVisual: StateFlow<FormularioSenderoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<FormularioSenderoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    // Sendero original en edición, para conservar sus datos no editables al guardar
    private var senderoOriginal: Sendero? = null
    // Cambia cada vez que se abre el menú de un punto de interés: una copia de foto que termina tarde no se pega a otro punto
    private var generacionPuntoNuevo = 0

    init {
        if (idSendero != null) cargarSendero(idSendero)
    }

    fun procesarEvento(evento: FormularioSenderoEvento) {
        when (evento) {
            is FormularioSenderoEvento.AlCambiarTexto -> cambiarTexto(evento.campo, evento.texto)
            is FormularioSenderoEvento.AlSeleccionarDificultad -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(dificultad = evento.dificultad, hayCambiosSinGuardar = true)
                }
            }
            is FormularioSenderoEvento.AlSeleccionarBioma -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(bioma = evento.bioma, hayCambiosSinGuardar = true)
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
                cancelarPuntoNuevo()
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(esEditorMapaVisible = false, indicePuntoSeleccionado = null)
                }
                completarDistanciaConLaDelMapa()
            }
            is FormularioSenderoEvento.AlAgregarPunto -> tocarMapa(evento.latitud, evento.longitud)
            is FormularioSenderoEvento.AlMoverPunto -> {
                editarRecorrido { it.mover(evento.indice, evento.latitud, evento.longitud) }
            }
            is FormularioSenderoEvento.AlSeleccionarPunto -> seleccionarPunto(evento.indice)
            is FormularioSenderoEvento.AlEliminarPunto -> editarRecorrido { it.eliminar(evento.indice) }
            is FormularioSenderoEvento.AlCambiarTipoPunto -> {
                editarRecorrido { it.reemplazar(evento.indice) { punto -> punto.copy(tipo = evento.tipo) } }
            }
            is FormularioSenderoEvento.AlCambiarNombrePunto -> {
                editarRecorrido { it.reemplazar(evento.indice) { punto -> punto.copy(nombre = evento.nombre) } }
            }
            is FormularioSenderoEvento.AlInvertirSentido -> editarRecorrido { it.invertir() }
            is FormularioSenderoEvento.AlInsertarPuntoAntes -> editarRecorrido { edicion -> edicion.insertarAntes(evento.indice) }
            is FormularioSenderoEvento.AlCambiarModoEditor -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(modoEditor = evento.modo, indicePuntoSeleccionado = null) }
            }
            is FormularioSenderoEvento.AlElegirTipoPuntoNuevo -> editarPuntoNuevo { punto -> punto.copy(tipo = evento.tipo) }
            is FormularioSenderoEvento.AlCambiarNombrePuntoNuevo -> editarPuntoNuevo { punto -> punto.copy(nombre = evento.nombre) }
            is FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo -> seleccionarFotoPuntoNuevo(evento.uri)
            is FormularioSenderoEvento.AlQuitarFotoPuntoNuevo -> quitarFotoPuntoNuevo()
            is FormularioSenderoEvento.AlFallarFotoPuntoNuevo -> editarPuntoNuevo { punto -> punto.copy(mensajeError = evento.mensaje) }
            is FormularioSenderoEvento.AlCancelarPuntoNuevo -> cancelarPuntoNuevo()
            is FormularioSenderoEvento.AlSeleccionarFotoDePunto -> seleccionarFotoDePunto(evento.indice, evento.uri)
            is FormularioSenderoEvento.AlQuitarFotoDePunto -> {
                editarRecorrido { edicion -> edicion.reemplazar(evento.indice) { punto -> punto.copy(foto = "") } }
            }
            is FormularioSenderoEvento.AlFallarFotoDePunto -> emitirEfecto(FormularioSenderoEfecto.MostrarAviso(evento.mensaje))
            is FormularioSenderoEvento.AlGuardarPuntoNuevo -> guardarPuntoNuevo()
            is FormularioSenderoEvento.AlSeleccionarFoto -> seleccionarFoto(evento.uri)
            is FormularioSenderoEvento.AlQuitarFoto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(foto = "", hayCambiosSinGuardar = true) }
            }
            is FormularioSenderoEvento.AlCancelarDescartar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoDescartarVisible = false) }
            }
        }
    }

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
                    bioma = senderoEncontrado.bioma,
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

    // Según el modo, agregar un punto al final del camino o abrir el menú para describir un punto de interés en ese lugar
    private fun tocarMapa(latitud: Double, longitud: Double) {
        val estadoActual = _estadoVisual.value
        if (estadoActual.modoEditor == ModoEditorRecorrido.TRAZAR_CAMINO) {
            editarRecorrido { edicion -> edicion.agregar(latitud, longitud) }
            return
        }
        if (estadoActual.puntoDeInteresNuevo != null) return
        generacionPuntoNuevo++
        _estadoVisual.update { estado ->
            estado.copy(puntoDeInteresNuevo = PuntoDeInteresNuevo(latitud = latitud, longitud = longitud), indicePuntoSeleccionado = null)
        }
    }

    // Cambiar un dato del punto de interés del menú; no hace nada si el menú está cerrado
    private fun editarPuntoNuevo(cambio: (PuntoDeInteresNuevo) -> PuntoDeInteresNuevo) {
        _estadoVisual.update { estadoActual ->
            estadoActual.puntoDeInteresNuevo?.let { punto -> estadoActual.copy(puntoDeInteresNuevo = cambio(punto)) } ?: estadoActual
        }
    }

    // Copiar la foto del punto de interés a la carpeta de la app; si había otra, la reemplaza y la borra
    private fun seleccionarFotoPuntoNuevo(uri: String) {
        val fotoAnterior = _estadoVisual.value.puntoDeInteresNuevo?.foto ?: return
        val generacion = generacionPuntoNuevo
        editarPuntoNuevo { punto -> punto.copy(estaProcesandoFoto = true, mensajeError = null) }
        viewModelScope.launch {
            almacenFotos.guardarCopia(uri).fold(
                onSuccess = { rutaLocal ->
                    if (_estadoVisual.value.puntoDeInteresNuevo == null || generacion != generacionPuntoNuevo) {
                        // El menú se cerró (o ya es otro punto) mientras se copiaba: la copia ya no tiene dueño
                        almacenFotos.borrar(rutaLocal)
                        return@fold
                    }
                    editarPuntoNuevo { punto -> punto.copy(foto = rutaLocal, estaProcesandoFoto = false) }
                    if (fotoAnterior.isNotEmpty()) almacenFotos.borrar(fotoAnterior)
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo copiar la foto del punto de interés", error)
                    if (generacion != generacionPuntoNuevo) return@fold
                    editarPuntoNuevo { punto -> punto.copy(estaProcesandoFoto = false, mensajeError = MENSAJE_FOTO_PUNTO_INVALIDA) }
                }
            )
        }
    }

    // Copiar la foto de un punto ya trazado y asignársela; si mientras tanto el punto se borró o cambió de lugar en la lista, la copia se descarta
    private fun seleccionarFotoDePunto(indice: Int, uri: String) {
        val estadoActual = _estadoVisual.value
        val punto = estadoActual.recorrido.getOrNull(indice) ?: return
        if (estadoActual.indicePuntoCopiandoFoto != null) return
        _estadoVisual.update { estado -> estado.copy(indicePuntoCopiandoFoto = indice) }
        viewModelScope.launch {
            almacenFotos.guardarCopia(uri).fold(
                onSuccess = { rutaLocal ->
                    val puntoActual = _estadoVisual.value.recorrido.getOrNull(indice)
                    val sigueEnSuLugar = puntoActual != null &&
                        puntoActual.latitud == punto.latitud && puntoActual.longitud == punto.longitud
                    _estadoVisual.update { estado -> estado.copy(indicePuntoCopiandoFoto = null) }
                    if (sigueEnSuLugar) {
                        editarRecorrido { edicion -> edicion.reemplazar(indice) { anterior -> anterior.copy(foto = rutaLocal) } }
                    } else {
                        almacenFotos.borrar(rutaLocal)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo copiar la foto del punto ${indice + 1}", error)
                    _estadoVisual.update { estado -> estado.copy(indicePuntoCopiandoFoto = null) }
                    canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(MENSAJE_FOTO_PUNTO_INVALIDA))
                }
            )
        }
    }

    // Quitar la foto del punto de interés y borrar su copia
    private fun quitarFotoPuntoNuevo() {
        val foto = _estadoVisual.value.puntoDeInteresNuevo?.foto.orEmpty()
        editarPuntoNuevo { punto -> punto.copy(foto = "") }
        if (foto.isNotEmpty()) viewModelScope.launch { almacenFotos.borrar(foto) }
    }

    // Cerrar el menú sin agregar el punto y borrar la foto que se había copiado para él
    private fun cancelarPuntoNuevo() {
        val foto = _estadoVisual.value.puntoDeInteresNuevo?.foto.orEmpty()
        _estadoVisual.update { estadoActual -> estadoActual.copy(puntoDeInteresNuevo = null) }
        if (foto.isNotEmpty()) viewModelScope.launch { almacenFotos.borrar(foto) }
    }

    // Insertar el punto de interés en el tramo más cercano del trazado y cerrar el menú; con la foto todavía copiándose espera
    private fun guardarPuntoNuevo() {
        val punto = _estadoVisual.value.puntoDeInteresNuevo ?: return
        if (punto.estaProcesandoFoto) return
        val puntoDelRecorrido = PuntoRecorrido(
            latitud = punto.latitud,
            longitud = punto.longitud,
            tipo = punto.tipo,
            nombre = punto.nombre.trim(),
            foto = punto.foto
        )
        editarRecorrido { edicion -> edicion.insertarPuntoDeInteres(puntoDelRecorrido, _estadoVisual.value.tipoRecorrido) }
        _estadoVisual.update { estadoActual -> estadoActual.copy(puntoDeInteresNuevo = null) }
        Log.d(TAG, "Punto de interés ${punto.tipo} agregado al recorrido")
    }

    private fun seleccionarPunto(indice: Int?) {
        if (indice != null && indice !in _estadoVisual.value.recorrido.indices) return
        _estadoVisual.update { estado -> estado.copy(indicePuntoSeleccionado = indice) }
    }

    // Solo con el campo vacío: una distancia ya cargada puede ser la del sendero completo y el trazado, apenas un tramo
    private fun completarDistanciaConLaDelMapa() {
        _estadoVisual.update { estadoActual ->
            if (estadoActual.distancia.isNotBlank() || estadoActual.recorrido.size < 2) return@update estadoActual
            val distanciaRedondeada = Math.round(estadoActual.distanciaCalculadaKilometros * 10) / 10.0
            estadoActual.copy(
                distancia = formatearDecimalEditable(distanciaRedondeada),
                camposConError = estadoActual.camposConError - CampoSendero.DISTANCIA,
                hayCambiosSinGuardar = true
            )
        }
    }

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

    private fun quitarEtapa(indice: Int) {
        _estadoVisual.update { estadoActual ->
            val etapasRestantes = estadoActual.etapas
                .filterIndexed { posicion, _ -> posicion != indice }
                .mapIndexed { posicion, etapa -> etapa.copy(numero = posicion + 1) }
            estadoActual.copy(etapas = etapasRestantes, hayCambiosSinGuardar = true)
        }
    }

    private fun interpretarDecimal(texto: String): Double? {
        val valor = texto.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return valor.takeIf { numero -> numero.isFinite() }
    }

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
            bioma = estadoActual.bioma,
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

    private fun volver() {
        if (_estadoVisual.value.hayCambiosSinGuardar) {
            _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoDescartarVisible = true) }
        } else {
            emitirEfecto(FormularioSenderoEfecto.VolverAtras)
        }
    }

    private fun emitirEfecto(efecto: FormularioSenderoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
