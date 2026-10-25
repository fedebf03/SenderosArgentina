package com.unaj.senderos.features.senderos.formulario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.recorrido.EdicionRecorrido
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraDuracion
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.port.ErrorElevacionException
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.domain.usecase.CalcularPerfilDeTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ConvertirCaminataEnBorradorUseCase
import com.unaj.senderos.core.domain.usecase.ImportarTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.RetirarPuntosIncorporadosUseCase
import com.unaj.senderos.core.domain.recorrido.TrazadoImportado
import kotlinx.coroutines.Job
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoInvalidoException
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
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
            // Un sendero nuevo arranca con las recomendaciones básicas. No cuentan como cambio, así no se pide confirmar al salir
            recomendaciones = if (idSendero == null) RECOMENDACIONES_POR_DEFECTO else emptyList()
        )
    )
    val estadoVisual: StateFlow<FormularioSenderoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<FormularioSenderoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    // Se conserva el sendero original para no perder sus datos no editables al guardar
    private var senderoOriginal: Sendero? = null
    // Aumenta cada vez que se abre el menú de un punto nuevo. Si una foto termina de copiarse tarde, no queda en otro punto
    private var generacionPuntoNuevo = 0
    private var trabajoPerfil: Job? = null

    // Puntos capturados de la caminata que ya van dentro del recorrido. Se retiran de los pendientes recién al guardar
    private var idsPuntosDeCaminata: Set<String> = emptySet()

    init {
        if (idSendero != null) cargarSendero(idSendero)
        if (idSendero == null && idCaminata != null) cargarCaminata(idCaminata)
    }

    fun procesarEvento(evento: FormularioSenderoEvento) {
        when (evento) {
            is FormularioSenderoEvento.AlCambiarTexto -> _estadoVisual.update { estado -> estado.conTexto(evento.campo, evento.texto) }
            is FormularioSenderoEvento.AlSeleccionarDificultad -> {
                _estadoVisual.update { estado -> estado.copy(dificultad = evento.dificultad, hayCambiosSinGuardar = true) }
            }
            is FormularioSenderoEvento.AlSeleccionarBioma -> {
                _estadoVisual.update { estado -> estado.copy(bioma = evento.bioma, hayCambiosSinGuardar = true) }
            }
            is FormularioSenderoEvento.AlSeleccionarTipoRecorrido -> {
                _estadoVisual.update { estado -> estado.copy(tipoRecorrido = evento.tipoRecorrido, hayCambiosSinGuardar = true) }
                // El tipo de recorrido cambia el trazado del perfil (cierre o vuelta). Con el mapa abierto se recalcula al cerrarlo
                if (!_estadoVisual.value.esEditorMapaVisible) calcularPerfil(forzar = false)
            }
            is FormularioSenderoEvento.AlAgregarRecomendacion -> _estadoVisual.update { estado -> estado.conRecomendacionNueva() }
            is FormularioSenderoEvento.AlAgregarRecomendacionSugerida -> {
                _estadoVisual.update { estado -> estado.conRecomendacionSugerida(evento.texto) }
            }
            is FormularioSenderoEvento.AlQuitarRecomendacion -> _estadoVisual.update { estado -> estado.sinRecomendacion(evento.indice) }
            is FormularioSenderoEvento.AlAgregarServicio -> _estadoVisual.update { estado -> estado.conServicioNuevo() }
            is FormularioSenderoEvento.AlAgregarServicioSugerido -> {
                _estadoVisual.update { estado -> estado.conServicioSugerido(evento.texto) }
            }
            is FormularioSenderoEvento.AlQuitarServicio -> _estadoVisual.update { estado -> estado.sinServicio(evento.indice) }
            is FormularioSenderoEvento.AlAgregarEtapa -> agregarEtapa()
            is FormularioSenderoEvento.AlQuitarEtapa -> _estadoVisual.update { estado -> estado.sinEtapa(evento.indice) }
            is FormularioSenderoEvento.AlCalcularDuracion -> calcularDuracion()
            is FormularioSenderoEvento.AlCalcularPerfil -> calcularPerfil(forzar = true)
            is FormularioSenderoEvento.AlImportarTrazado -> importarTrazado(evento.uri)
            is FormularioSenderoEvento.AlConfirmarReemplazoTrazado -> {
                _estadoVisual.value.trazadoImportadoPendiente?.let { trazado -> aplicarTrazadoImportado(trazado) }
            }
            is FormularioSenderoEvento.AlCancelarReemplazoTrazado -> {
                _estadoVisual.update { estado -> estado.copy(trazadoImportadoPendiente = null) }
            }
            is FormularioSenderoEvento.AlReemplazarSenderoDeCaminata -> {
                _estadoVisual.update { estado -> estado.copy(nombreSenderoYaConvertido = null) }
            }
            is FormularioSenderoEvento.AlEditarSenderoDeCaminata -> {
                idCaminata?.let { id -> emitirEfecto(FormularioSenderoEfecto.EditarSenderoExistente(IdentificadorCaminataLibre.idSenderoDesde(id))) }
            }
            is FormularioSenderoEvento.AlGuardar -> guardar()
            is FormularioSenderoEvento.AlVolver -> volver()
            is FormularioSenderoEvento.AlConfirmarDescartar -> {
                _estadoVisual.update { estado -> estado.copy(esDialogoDescartarVisible = false) }
                emitirEfecto(FormularioSenderoEfecto.VolverAtras)
            }
            is FormularioSenderoEvento.AlCancelarDescartar -> {
                _estadoVisual.update { estado -> estado.copy(esDialogoDescartarVisible = false) }
            }
            is FormularioSenderoEvento.AlAbrirEditorMapa -> {
                _estadoVisual.update { estado -> estado.copy(esEditorMapaVisible = true) }
            }
            is FormularioSenderoEvento.AlCerrarEditorMapa -> {
                cancelarPuntoNuevo()
                _estadoVisual.update { estado -> estado.copy(esEditorMapaVisible = false, indicePuntoSeleccionado = null) }
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
                _estadoVisual.update { estado -> estado.copy(modoEditor = evento.modo, indicePuntoSeleccionado = null) }
            }
            is FormularioSenderoEvento.AlElegirTipoPuntoNuevo -> editarPuntoNuevo { punto -> punto.copy(tipo = evento.tipo) }
            is FormularioSenderoEvento.AlCambiarNombrePuntoNuevo -> editarPuntoNuevo { punto -> punto.copy(nombre = evento.nombre) }
            is FormularioSenderoEvento.AlSeleccionarFotoPuntoNuevo -> seleccionarFotoPuntoNuevo(evento.uri)
            is FormularioSenderoEvento.AlQuitarFotoPuntoNuevo -> quitarFotoPuntoNuevo()
            is FormularioSenderoEvento.AlFallarFotoPuntoNuevo -> editarPuntoNuevo { punto -> punto.copy(mensajeError = evento.mensaje) }
            is FormularioSenderoEvento.AlCancelarPuntoNuevo -> cancelarPuntoNuevo()
            is FormularioSenderoEvento.AlGuardarPuntoNuevo -> guardarPuntoNuevo()
            is FormularioSenderoEvento.AlSeleccionarFotoDePunto -> seleccionarFotoDePunto(evento.indice, evento.uri)
            is FormularioSenderoEvento.AlQuitarFotoDePunto -> {
                editarRecorrido { edicion -> edicion.reemplazar(evento.indice) { punto -> punto.copy(foto = "") } }
            }
            is FormularioSenderoEvento.AlFallarFotoDePunto -> emitirEfecto(FormularioSenderoEfecto.MostrarAviso(evento.mensaje))
            is FormularioSenderoEvento.AlSeleccionarFoto -> seleccionarFoto(evento.uri)
            is FormularioSenderoEvento.AlQuitarFoto -> {
                _estadoVisual.update { estado -> estado.copy(foto = "", hayCambiosSinGuardar = true) }
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
            _estadoVisual.update { estado -> estado.conSenderoCargado(senderoEncontrado) }
        }
    }

    private fun mostrarErrorDeCarga(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaCargando = false, mensajeError = mensaje) }
    }

    private fun editarRecorrido(operacion: (EdicionRecorrido) -> EdicionRecorrido) {
        _estadoVisual.update { estado -> estado.conRecorridoEditado(operacion) }
    }

    // RF4 - Editor de recorrido: según el modo, un toque agrega un punto al camino o abre el menú de un punto de interés
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

    private fun editarPuntoNuevo(cambio: (PuntoDeInteresNuevo) -> PuntoDeInteresNuevo) {
        _estadoVisual.update { estadoActual ->
            estadoActual.puntoDeInteresNuevo?.let { punto -> estadoActual.copy(puntoDeInteresNuevo = cambio(punto)) } ?: estadoActual
        }
    }

    // RF6 - Fotos: la foto del punto de interés se copia a la carpeta de la app y la anterior se borra
    private fun seleccionarFotoPuntoNuevo(uri: String) {
        val fotoAnterior = _estadoVisual.value.puntoDeInteresNuevo?.foto ?: return
        val generacion = generacionPuntoNuevo
        editarPuntoNuevo { punto -> punto.copy(estaProcesandoFoto = true, mensajeError = null) }
        viewModelScope.launch {
            almacenFotos.guardarCopia(uri).fold(
                onSuccess = { rutaLocal ->
                    if (_estadoVisual.value.puntoDeInteresNuevo == null || generacion != generacionPuntoNuevo) {
                        // El menú se cerró (o ya es otro punto) mientras se copiaba. Por lo tanto la copia ya no tiene dueño
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

    // Si mientras se copiaba el punto se borró o cambió de lugar en la lista, la copia se descarta
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

    private fun quitarFotoPuntoNuevo() {
        val foto = _estadoVisual.value.puntoDeInteresNuevo?.foto.orEmpty()
        editarPuntoNuevo { punto -> punto.copy(foto = "") }
        borrarFoto(foto)
    }

    private fun cancelarPuntoNuevo() {
        val foto = _estadoVisual.value.puntoDeInteresNuevo?.foto.orEmpty()
        _estadoVisual.update { estadoActual -> estadoActual.copy(puntoDeInteresNuevo = null) }
        borrarFoto(foto)
    }

    private fun borrarFoto(ruta: String) {
        if (ruta.isNotEmpty()) viewModelScope.launch { almacenFotos.borrar(ruta) }
    }

    // Con la foto todavía copiándose no se inserta el punto, porque quedaría sin su foto
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

    // RF4 - Formulario: el perfil de elevación se pide solo si cambió la firma del trazado (o si se fuerza).
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
            resultado.getOrNull()?.let { perfil -> Log.d(TAG, "Perfil de elevación calculado: ${perfil.desnivelPositivoMetros} m de subida") }
            _estadoVisual.update { estado ->
                val sigueElMismoTrazado = CalculadoraPerfil.firmaTrazado(estado.recorrido, estado.tipoRecorrido) == firma
                when {
                    !sigueElMismoTrazado -> estado.copy(estaCalculandoPerfil = false)
                    resultado.isSuccess -> estado.conPerfil(resultado.getOrThrow())
                    else -> estado.copy(estaCalculandoPerfil = false, avisoPerfil = describirFalloDelPerfil(resultado.exceptionOrNull()))
                }
            }
        }
    }

    // RF4 - Formulario: la caminata libre se convierte en el recorrido del sendero nuevo. Si falla, se avisa el motivo
    private fun cargarCaminata(idCaminataOrigen: String) {
        viewModelScope.launch {
            avisarSiLaCaminataYaSeConvirtio(idCaminataOrigen)
            capturarFallos { convertirCaminataEnBorradorUseCase(idCaminataOrigen) }.fold(
                onSuccess = { borrador ->
                    Log.d(TAG, "Caminata $idCaminataOrigen cargada: ${borrador.trazado.recorrido.size} puntos")
                    idsPuntosDeCaminata = borrador.idsPuntosCapturados
                    aplicarTrazadoImportado(borrador.trazado, "Caminata cargada")
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo cargar la caminata $idCaminataOrigen", error)
                    canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(mensajeParaUsuario(error, MENSAJE_CAMINATA_ERROR)))
                }
            )
        }
    }

    // Si esta caminata ya se convirtió en sendero, se pregunta si se edita o se reemplaza antes de pisarlo
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

    // RF4 - Importar GPX: si ya hay un recorrido marcado, se pide confirmar antes de reemplazarlo
    private fun importarTrazado(uri: String) {
        if (_estadoVisual.value.estaImportandoTrazado) return
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaImportandoTrazado = true) }
        viewModelScope.launch {
            val resultado = capturarFallos { importarTrazadoUseCase(uri) }
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
                    canalEfectos.send(FormularioSenderoEfecto.MostrarAviso(mensajeParaUsuario(error, MENSAJE_IMPORTACION_ERROR)))
                }
            )
        }
    }

    // Sin alturas en el archivo, el perfil se pide a la red como al cerrar el editor
    private fun aplicarTrazadoImportado(trazado: TrazadoImportado, origen: String = "Recorrido importado") {
        _estadoVisual.update { estado -> estado.conTrazadoImportado(trazado) }
        val perfil = trazado.perfil
        if (perfil != null) {
            Log.d(TAG, "Perfil de elevación calculado: ${perfil.desnivelPositivoMetros} m de subida")
            // Un cálculo que siga en curso era del trazado anterior
            trabajoPerfil?.cancel()
        } else {
            calcularPerfil(forzar = false)
        }
        emitirEfecto(FormularioSenderoEfecto.MostrarAviso("$origen: ${trazado.recorrido.size} puntos"))
    }

    private fun describirFalloDelPerfil(error: Throwable?): String {
        if (error != null) Log.e(TAG, "No se pudo calcular el perfil de elevación", error)
        return when (error) {
            is SinConexionElevacionException -> MENSAJE_PERFIL_SIN_CONEXION
            is ErrorElevacionException -> error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_PERFIL_ERROR
            else -> MENSAJE_PERFIL_ERROR
        }
    }

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

    private fun agregarEtapa() {
        if (_estadoVisual.value.conEtapaNueva() == null) {
            emitirEfecto(FormularioSenderoEfecto.MostrarAviso(MENSAJE_ETAPA_INVALIDA))
            return
        }
        _estadoVisual.update { estado -> estado.conEtapaNueva() ?: estado }
    }

    // RF4 - Formulario: guardamos el sendero armado con su perfil. Un perfil que todavía se calcula se abandona
    private fun guardar() {
        val estadoActual = _estadoVisual.value
        if (estadoActual.estaGuardando || estadoActual.estaCargando || estadoActual.mensajeError != null) return
        if (estadoActual.estaProcesandoFoto) return

        val senderoArmado = estadoActual.armarSendero(senderoOriginal, idCaminata)
        if (senderoArmado == null) {
            _estadoVisual.update { estadoPrevio -> estadoPrevio.copy(camposConError = setOf(CampoSendero.RECORRIDO)) }
            emitirEfecto(FormularioSenderoEfecto.MostrarAviso(MENSAJE_FALTA_RECORRIDO))
            return
        }

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

    // Si falla solo se registra, porque el sendero ya está guardado y los puntos se pueden volver a ver desde el historial
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

    // RF6 - Fotos: la foto se copia a la app para que siga disponible aunque el usuario la borre de la galería
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

    // Los fallos inesperados se convierten en un Result fallido. En cambio, la cancelación sigue su curso
    private suspend fun <T> capturarFallos(operacion: suspend () -> Result<T>): Result<T> = try {
        operacion()
    } catch (excepcion: CancellationException) {
        throw excepcion
    } catch (error: Exception) {
        Result.failure(error)
    }

    // Solo los fallos propios traen un mensaje pensado para el usuario. Los técnicos muestran el genérico
    private fun mensajeParaUsuario(error: Throwable, porDefecto: String): String {
        val mensaje = if (error is IllegalArgumentException || error is IllegalStateException) error.message else null
        return mensaje?.takeIf { texto -> texto.isNotBlank() } ?: porDefecto
    }
}
