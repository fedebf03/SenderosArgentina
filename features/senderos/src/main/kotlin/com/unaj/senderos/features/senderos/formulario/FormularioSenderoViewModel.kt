package com.unaj.senderos.features.senderos.formulario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.recorrido.EdicionRecorrido
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraDuracion
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.port.ErrorElevacionException
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.domain.usecase.CalcularPerfilDeTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ConvertirCaminataEnBorradorUseCase
import com.unaj.senderos.core.domain.usecase.ImportarTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.RetirarPuntosIncorporadosUseCase
import com.unaj.senderos.core.domain.recorrido.TrazadoImportado
import kotlinx.coroutines.Job
import kotlin.math.roundToInt
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoInvalidoException
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.ui.comun.formatearDecimalEditable
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
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
private const val MENSAJE_FALTA_DISTANCIA_PARA_DURACION = "Marcá el recorrido para calcular la duración"
private const val MENSAJE_FALTA_RECORRIDO = "Marcá el recorrido en el mapa para guardar el sendero"
private const val MENSAJE_PERFIL_SIN_CONEXION = "Sin conexión: el perfil se calculará al sincronizar"
private const val MENSAJE_PERFIL_ERROR = "No se pudo calcular el perfil de elevación"
private const val MENSAJE_IMPORTACION_ERROR = "No se pudo importar el archivo"
private const val MENSAJE_CAMINATA_ERROR = "No se pudo cargar la caminata"

@HiltViewModel(assistedFactory = FormularioSenderoViewModel.Fabrica::class)
class FormularioSenderoViewModel @AssistedInject constructor(
    @Assisted("idSendero") private val idSendero: String?,
    @Assisted("idCaminata") private val idCaminata: String?,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val guardarSenderoUseCase: GuardarSenderoUseCase,
    private val almacenFotos: AlmacenFotos,
    private val calcularPerfilDeTrazadoUseCase: CalcularPerfilDeTrazadoUseCase,
    private val importarTrazadoUseCase: ImportarTrazadoUseCase,
    private val convertirCaminataEnBorradorUseCase: ConvertirCaminataEnBorradorUseCase,
    private val retirarPuntosIncorporadosUseCase: RetirarPuntosIncorporadosUseCase
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(
            @Assisted("idSendero") idSendero: String?,
            @Assisted("idCaminata") idCaminata: String? = null
        ): FormularioSenderoViewModel
    }

    private val _estadoVisual = MutableStateFlow(
        FormularioSenderoEstadoVisual(
            esEdicion = idSendero != null,
            estaCargando = idSendero != null,
            // Un sendero nuevo arranca con las recomendaciones básicas; no cuentan como cambio para no pedir confirmación al salir
            recomendaciones = if (idSendero == null) RECOMENDACIONES_POR_DEFECTO else emptyList()
        )
    )
    val estadoVisual: StateFlow<FormularioSenderoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<FormularioSenderoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    // Sendero original en edición, para conservar sus datos no editables al guardar
    private var senderoOriginal: Sendero? = null
    // Cambia cada vez que se abre el menú de un punto de interés: una copia de foto que termina tarde no se pega a otro punto
    private var generacionPuntoNuevo = 0
    // Cálculo del perfil en curso; uno nuevo cancela al anterior
    private var trabajoPerfil: Job? = null

    // Puntos capturados de la caminata de origen que ya van dentro del recorrido; se retiran de los pendientes cuando el sendero se guarda
    private var idsPuntosDeCaminata: Set<String> = emptySet()

    init {
        if (idSendero != null) cargarSendero(idSendero)
        if (idSendero == null && idCaminata != null) cargarCaminata(idCaminata)
    }

    // Procesar cada evento de la pantalla y traducirlo en cambios del estado del formulario
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
                // El tipo cambia el trazado del perfil (cierre o vuelta); con el mapa abierto se recalcula al cerrarlo
                if (!_estadoVisual.value.esEditorMapaVisible) calcularPerfil(forzar = false)
            }
            is FormularioSenderoEvento.AlAgregarRecomendacion -> agregarRecomendacion()
            is FormularioSenderoEvento.AlAgregarRecomendacionSugerida -> {
                _estadoVisual.update { estadoActual ->
                    agregarSiFalta(estadoActual.recomendaciones, evento.texto)
                        ?.let { lista -> estadoActual.copy(recomendaciones = lista, hayCambiosSinGuardar = true) } ?: estadoActual
                }
            }
            is FormularioSenderoEvento.AlAgregarServicioSugerido -> {
                _estadoVisual.update { estadoActual ->
                    agregarSiFalta(estadoActual.servicios, evento.texto)
                        ?.let { lista -> estadoActual.copy(servicios = lista, hayCambiosSinGuardar = true) } ?: estadoActual
                }
            }
            is FormularioSenderoEvento.AlCalcularDuracion -> calcularDuracion()
            is FormularioSenderoEvento.AlCalcularPerfil -> calcularPerfil(forzar = true)
            is FormularioSenderoEvento.AlImportarTrazado -> importarTrazado(evento.uri)
            is FormularioSenderoEvento.AlConfirmarReemplazoTrazado -> {
                _estadoVisual.value.trazadoImportadoPendiente?.let { trazado -> aplicarTrazadoImportado(trazado) }
            }
            is FormularioSenderoEvento.AlReemplazarSenderoDeCaminata -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(nombreSenderoYaConvertido = null) }
            }
            is FormularioSenderoEvento.AlEditarSenderoDeCaminata -> {
                idCaminata?.let { id -> emitirEfecto(FormularioSenderoEfecto.EditarSenderoExistente(IdentificadorCaminataLibre.idSenderoDesde(id))) }
            }
            is FormularioSenderoEvento.AlCancelarReemplazoTrazado -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(trazadoImportadoPendiente = null) }
            }
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
                calcularPerfil(forzar = false)
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

    // Cargar el sendero a editar en el formulario, con su perfil y si la distancia sigue o no a la del mapa
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
                    perfilElevacion = senderoEncontrado.perfilElevacion,
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
                estado.copy(
                    recorrido = editado.puntos,
                    indicePuntoSeleccionado = editado.seleccion,
                    camposConError = estado.camposConError - CampoSendero.RECORRIDO,
                    hayCambiosSinGuardar = true
                )
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

    // Pedir las alturas del trazado y armar el perfil; sin forzar, no hace nada si el perfil ya corresponde al trazado.
    // Si el trazado cambia mientras se consulta, el resultado se descarta
    private fun calcularPerfil(forzar: Boolean) {
        val estadoActual = _estadoVisual.value
        if (estadoActual.recorrido.size < 2) return
        val firma = CalculadoraPerfil.firmaTrazado(estadoActual.recorrido, estadoActual.tipoRecorrido)
        if (!forzar && estadoActual.perfilElevacion?.firmaTrazado == firma) return
        trabajoPerfil?.cancel()
        _estadoVisual.update { estado -> estado.copy(estaCalculandoPerfil = true, avisoPerfil = null) }
        val recorrido = estadoActual.recorrido
        val tipoRecorrido = estadoActual.tipoRecorrido
        trabajoPerfil = viewModelScope.launch {
            val resultado = calcularPerfilDeTrazadoUseCase(recorrido, tipoRecorrido)
            _estadoVisual.update { estado ->
                val sigueElMismoTrazado = CalculadoraPerfil.firmaTrazado(estado.recorrido, estado.tipoRecorrido) == firma
                when {
                    !sigueElMismoTrazado -> estado.copy(estaCalculandoPerfil = false)
                    resultado.isSuccess -> aplicarPerfil(estado, resultado.getOrThrow())
                    else -> estado.copy(estaCalculandoPerfil = false, avisoPerfil = describirFalloDelPerfil(resultado.exceptionOrNull()))
                }
            }
        }
    }

    // Convertir la caminata libre de origen en el recorrido del sendero nuevo; si no se puede, el formulario queda vacío y se avisa el motivo
    private fun cargarCaminata(idCaminataOrigen: String) {
        viewModelScope.launch {
            avisarSiLaCaminataYaSeConvirtio(idCaminataOrigen)
            val resultado = try {
                convertirCaminataEnBorradorUseCase(idCaminataOrigen)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            resultado.fold(
                onSuccess = { borrador ->
                    Log.d(TAG, "Caminata $idCaminataOrigen cargada: ${borrador.trazado.recorrido.size} puntos")
                    idsPuntosDeCaminata = borrador.idsPuntosCapturados
                    aplicarTrazadoImportado(borrador.trazado, "Caminata cargada")
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo cargar la caminata $idCaminataOrigen", error)
                    val mensaje = if (error is IllegalArgumentException || error is IllegalStateException) error.message else null
                    canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(mensaje?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_CAMINATA_ERROR))
                }
            )
        }
    }

    // Si ya existe el sendero creado desde esta caminata, guardar su nombre para preguntar si se edita o se reemplaza antes de pisarlo
    private suspend fun avisarSiLaCaminataYaSeConvirtio(idCaminataOrigen: String) {
        val existente = try {
            observarDetalleSenderoUseCase(IdentificadorCaminataLibre.idSenderoDesde(idCaminataOrigen)).getOrNull()?.first()
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo comprobar si la caminata ya se convirtió", error)
            null
        }
        if (existente != null) {
            Log.d(TAG, "La caminata $idCaminataOrigen ya es el sendero ${existente.id}")
            _estadoVisual.update { estadoActual -> estadoActual.copy(nombreSenderoYaConvertido = existente.nombre) }
        }
    }

    // Leer el archivo elegido e importar su trazado; si ya hay un recorrido marcado, pide confirmar antes de reemplazarlo
    private fun importarTrazado(uri: String) {
        if (_estadoVisual.value.estaImportandoTrazado) return
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaImportandoTrazado = true) }
        viewModelScope.launch {
            val resultado = try {
                importarTrazadoUseCase(uri)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            _estadoVisual.update { estadoActual -> estadoActual.copy(estaImportandoTrazado = false) }
            resultado.fold(
                onSuccess = { trazado ->
                    Log.d(TAG, "Trazado importado: ${trazado.recorrido.size} puntos, ${trazado.tipoRecorrido}")
                    if (_estadoVisual.value.recorrido.isEmpty()) {
                        aplicarTrazadoImportado(trazado)
                    } else {
                        _estadoVisual.update { estadoActual -> estadoActual.copy(trazadoImportadoPendiente = trazado) }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo importar el trazado", error)
                    // Solo los fallos propios traen un mensaje pensado para el usuario; los técnicos muestran el genérico
                    val mensaje = if (error is IllegalArgumentException || error is IllegalStateException) error.message else null
                    canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(mensaje?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_IMPORTACION_ERROR))
                }
            )
        }
    }

    // Cargar el trazado importado: recorrido, tipo y nombre si estaba vacío; con alturas en el archivo usa ese
    // perfil y, si no, lo pide a la red como al cerrar el editor
    private fun aplicarTrazadoImportado(trazado: TrazadoImportado, origen: String = "Recorrido importado") {
        _estadoVisual.update { estadoActual ->
            val conTrazado = estadoActual.copy(
                recorrido = trazado.recorrido,
                tipoRecorrido = trazado.tipoRecorrido,
                nombre = estadoActual.nombre.ifBlank { trazado.nombre },
                indicePuntoSeleccionado = null,
                trazadoImportadoPendiente = null,
                camposConError = estadoActual.camposConError - CampoSendero.RECORRIDO - CampoSendero.NOMBRE,
                hayCambiosSinGuardar = true
            )
            trazado.perfil?.let { perfil -> aplicarPerfil(conTrazado, perfil) } ?: conTrazado
        }
        // Un cálculo anterior ya no corresponde a este trazado
        if (trazado.perfil != null) trabajoPerfil?.cancel()
        if (trazado.perfil == null) calcularPerfil(forzar = false)
        emitirEfecto(FormularioSenderoEfecto.MostrarAviso("$origen: ${trazado.recorrido.size} puntos"))
    }

    private fun aplicarPerfil(estado: FormularioSenderoEstadoVisual, perfil: PerfilElevacion): FormularioSenderoEstadoVisual {
        Log.d(TAG, "Perfil de elevación calculado: ${perfil.desnivelPositivoMetros} m de subida")
        return estado.copy(
            perfilElevacion = perfil,
            estaCalculandoPerfil = false,
            avisoPerfil = null,
            hayCambiosSinGuardar = true
        )
    }

    // Traducir el fallo del perfil en un aviso para el usuario
    private fun describirFalloDelPerfil(error: Throwable?): String {
        if (error != null) Log.e(TAG, "No se pudo calcular el perfil de elevación", error)
        return when (error) {
            is SinConexionElevacionException -> MENSAJE_PERFIL_SIN_CONEXION
            is ErrorElevacionException -> error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_PERFIL_ERROR
            else -> MENSAJE_PERFIL_ERROR
        }
    }

    // Cambiar el texto de un campo, limpiar su error y marcar el cambio si modifica datos del sendero
    private fun cambiarTexto(campo: CampoFormulario, texto: String) {
        _estadoVisual.update { estadoActual ->
            val estadoConTexto = when (campo) {
                CampoFormulario.NOMBRE -> estadoActual.copy(nombre = texto)
                CampoFormulario.REGION -> estadoActual.copy(region = texto)
                CampoFormulario.DESCRIPCION -> estadoActual.copy(descripcion = texto)
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
            else -> null
        }
    }

    // Devolver la lista con el texto agregado al final, o null si ya estaba (sin distinguir mayúsculas)
    private fun agregarSiFalta(lista: List<String>, texto: String): List<String>? {
        val limpio = texto.trim()
        if (limpio.isEmpty() || lista.any { elemento -> elemento.equals(limpio, ignoreCase = true) }) return null
        return lista + limpio
    }

    // Estimar la duración con la distancia del recorrido marcado y el desnivel, y escribirla en el campo
    private fun calcularDuracion() {
        val estadoActual = _estadoVisual.value
        val distancia = estadoActual.distanciaCalculadaKilometros
        val desnivel = estadoActual.perfilElevacion?.desnivelPositivoMetros ?: 0
        val minutos = CalculadoraDuracion.estimarMinutos(distancia, desnivel)
        if (minutos == null) {
            emitirEfecto(FormularioSenderoEfecto.MostrarAviso(MENSAJE_FALTA_DISTANCIA_PARA_DURACION))
            return
        }
        Log.d(TAG, "Duración estimada: $minutos min para $distancia km y $desnivel m de subida")
        _estadoVisual.update { estado ->
            estado.copy(duracionEstimada = formatearDuracionEstimada(minutos), hayCambiosSinGuardar = true)
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

    // Validar los números, armar el sendero con el perfil y guardarlo; un perfil que todavía se calcula se abandona
    private fun guardar() {
        val estadoActual = _estadoVisual.value
        if (estadoActual.estaGuardando || estadoActual.estaCargando || estadoActual.mensajeError != null) return
        if (estadoActual.estaProcesandoFoto) return

        // La distancia sale siempre del recorrido; un sendero viejo sin trazado conserva la que ya tenía
        val distancia = if (estadoActual.recorrido.size >= 2) {
            Math.round(estadoActual.distanciaCalculadaKilometros * 10) / 10.0
        } else {
            senderoOriginal?.distanciaKilometros?.takeIf { valor -> valor > 0.0 }
        }
        if (distancia == null) {
            _estadoVisual.update { estadoPrevio -> estadoPrevio.copy(camposConError = setOf(CampoSendero.RECORRIDO)) }
            emitirEfecto(FormularioSenderoEfecto.MostrarAviso(MENSAJE_FALTA_RECORRIDO))
            return
        }

        val senderoBase = senderoOriginal ?: Sendero(
            id = idCaminata?.let { id -> IdentificadorCaminataLibre.idSenderoDesde(id) }.orEmpty(),
            idUsuario = "",
            nombre = "",
            region = "",
            distanciaKilometros = 0.0,
            dificultad = estadoActual.dificultad
        )
        val senderoSinPerfil = senderoBase.copy(
            nombre = estadoActual.nombre,
            region = estadoActual.region,
            descripcion = estadoActual.descripcion,
            distanciaKilometros = distancia,
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
        // Viaja aunque sea de otro trazado: su firma distinta hace que la sincronización lo recalcule
        val senderoArmado = estadoActual.perfilElevacion
            ?.let { perfil -> CalculadoraPerfil.aplicarAlSendero(senderoSinPerfil, perfil) }
            ?: senderoSinPerfil

        trabajoPerfil?.cancel()
        _estadoVisual.update { estadoPrevio ->
            estadoPrevio.copy(estaGuardando = true, camposConError = emptySet(), estaCalculandoPerfil = false)
        }
        viewModelScope.launch {
            guardarSenderoUseCase(senderoArmado).fold(
                onSuccess = { senderoGuardado ->
                    _estadoVisual.update { estadoPrevio ->
                        estadoPrevio.copy(estaGuardando = false, hayCambiosSinGuardar = false)
                    }
                    retirarPuntosDeLaCaminata()
                    canalEfectos.send(FormularioSenderoEfecto.SenderoGuardado(senderoGuardado.id))
                },
                onFailure = { error -> manejarFalloDeGuardado(error) }
            )
        }
    }

    // Retirar de los pendientes los puntos de la caminata de origen, que ya quedaron dentro del sendero guardado; si falla solo se registra,
    // porque el sendero ya está guardado y los puntos pendientes se pueden volver a ver desde el historial
    private suspend fun retirarPuntosDeLaCaminata() {
        if (idsPuntosDeCaminata.isEmpty()) return
        retirarPuntosIncorporadosUseCase(idsPuntosDeCaminata).onFailure { error ->
            Log.e(TAG, "No se pudieron retirar los puntos de la caminata", error)
        }
        idsPuntosDeCaminata = emptySet()
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
