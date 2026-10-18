package com.unaj.senderos.features.senderos.listado

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.port.ControlSeguimiento
import com.unaj.senderos.core.domain.port.LanzadorSeguimiento
import com.unaj.senderos.core.domain.port.MonitorConectividad
import com.unaj.senderos.core.domain.port.Reloj
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.port.ProveedorUbicacion
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosPublicosUseCase
import com.unaj.senderos.core.ui.comun.observarSenderos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

private const val TAG = "SENDEROS"
private const val NOMBRE_CAMINATA_LIBRE = "Caminata libre"
private const val MENSAJE_SIN_PERMISO_PARA_CAMINAR = "Sin el permiso de ubicación precisa no podemos registrar tu caminata"
private const val MENSAJE_RECORRIDO_EN_CURSO = "Ya tenés un recorrido en curso: terminalo antes de empezar una caminata"
private const val MENSAJE_SIN_SESION = "Iniciá sesión para empezar una caminata"

@HiltViewModel(assistedFactory = SenderosViewModel.Fabrica::class)
class SenderosViewModel @AssistedInject constructor(
    @Assisted modo: ModoListado,
    observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase,
    observarSenderosPublicosUseCase: ObservarSenderosPublicosUseCase,
    private val proveedorUbicacion: ProveedorUbicacion,
    monitorConectividad: MonitorConectividad,
    private val controlSeguimiento: ControlSeguimiento,
    private val lanzadorSeguimiento: LanzadorSeguimiento,
    private val obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase,
    private val reloj: Reloj
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(modo: ModoListado): SenderosViewModel
    }

    private val _estadoVisual = MutableStateFlow(SenderosEstadoVisual(modo = modo))
    val estadoVisual: StateFlow<SenderosEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<SenderosEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private var trabajoUbicacion: Job? = null

    init {
        observarConexion(monitorConectividad)
        if (modo == ModoListado.EXPLORAR) observarPublicos(observarSenderosPublicosUseCase)
        observarSenderos(
            observarSenderosDelUsuarioUseCase,
            alRecibir = { datos ->
                _estadoVisual.update { estado ->
                    estado.copy(
                        usuario = datos.usuario,
                        senderos = datos.senderos,
                        estaCargando = estado.estaCargando && modo == ModoListado.EXPLORAR
                    )
                }
            },
            alFallar = { mensaje ->
                _estadoVisual.update { estado -> estado.copy(estaCargando = false, mensajeError = mensaje) }
            }
        )
    }

    // Se avisa sin bloquear la lista
    private fun observarConexion(monitorConectividad: MonitorConectividad) {
        viewModelScope.launch {
            monitorConectividad.observarConexion()
                .catch { error -> Log.e(TAG, "No se pudo observar la conexión", error) }
                .collect { hayConexion ->
                    _estadoVisual.update { estado -> estado.copy(estaSinConexion = !hayConexion) }
                }
        }
    }

    fun procesarEvento(evento: SenderosEvento) {
        when (evento) {
            is SenderosEvento.AlCambiarBusqueda -> {
                _estadoVisual.update { estado -> estado.copy(textoBusqueda = evento.texto) }
            }
            is SenderosEvento.AlCambiarOrdenCercania -> {
                Log.d(TAG, "Orden por cercanía: ${evento.activado}")
                _estadoVisual.update { estado -> estado.copy(ordenarPorCercania = evento.activado) }
            }
            is SenderosEvento.AlAlternarDificultad -> {
                _estadoVisual.update { estado ->
                    val seleccionadas = estado.dificultadesSeleccionadas
                    estado.copy(
                        dificultadesSeleccionadas = if (evento.dificultad in seleccionadas) {
                            seleccionadas - evento.dificultad
                        } else {
                            seleccionadas + evento.dificultad
                        }
                    )
                }
            }
            is SenderosEvento.AlAlternarFavoritos -> {
                _estadoVisual.update { estado -> estado.copy(soloFavoritos = !estado.soloFavoritos) }
            }
            is SenderosEvento.AlAlternarGuardados -> {
                _estadoVisual.update { estado -> estado.copy(soloGuardados = !estado.soloGuardados) }
            }
            is SenderosEvento.AlObtenerUbicacion -> obtenerUbicacion()
            is SenderosEvento.AlSolicitarPermisoNotificaciones -> {
                _estadoVisual.update { estado -> estado.copy(seSolicitoPermisoNotificaciones = true) }
            }
            is SenderosEvento.AlTocarCrearSendero -> emitir(SenderosEfecto.NavegarACreacion)
            is SenderosEvento.AlTocarCaminarAhora -> emitir(SenderosEfecto.SolicitarPermisoUbicacionParaCaminar)
            is SenderosEvento.AlCaminarAhora -> caminarAhora()
            is SenderosEvento.AlDenegarPermisoParaCaminar -> emitir(SenderosEfecto.MostrarAviso(MENSAJE_SIN_PERMISO_PARA_CAMINAR))
            is SenderosEvento.AlSeleccionarSendero -> {
                val esPublicoDeOtro = _estadoVisual.value.esPublicoDeOtro(evento.idSendero)
                emitir(
                    if (esPublicoDeOtro) {
                        SenderosEfecto.NavegarADetallePublico(evento.idSendero)
                    } else {
                        SenderosEfecto.NavegarADetalle(evento.idSendero)
                    }
                )
            }
        }
    }

    // Iniciar una caminata libre con un identificador nuevo y abrir su pantalla; si ya hay una caminata libre en curso vuelve a ella, y con un
    // recorrido de sendero en curso avisa en lugar de reemplazarlo en silencio
    private fun caminarAhora() {
        val enCurso = controlSeguimiento.estado.value
        if (enCurso != null) {
            if (enCurso.esCaminataLibre) {
                emitir(SenderosEfecto.NavegarACaminata(enCurso.idSendero))
            } else {
                emitir(SenderosEfecto.MostrarAviso(MENSAJE_RECORRIDO_EN_CURSO))
            }
            return
        }
        val usuario = obtenerUsuarioActualUseCase()
        if (usuario == null) {
            emitir(SenderosEfecto.MostrarAviso(MENSAJE_SIN_SESION))
            return
        }
        val idCaminata = IdentificadorCaminataLibre.crear(reloj())
        Log.d(TAG, "Se inicia una caminata libre: $idCaminata")
        lanzadorSeguimiento.iniciarCaminataLibre(idCaminata, usuario.id, NOMBRE_CAMINATA_LIBRE)
        emitir(SenderosEfecto.NavegarACaminata(idCaminata))
    }

    private fun observarPublicos(casoDeUso: ObservarSenderosPublicosUseCase) {
        casoDeUso().fold(
            onSuccess = { flujo ->
                viewModelScope.launch {
                    flujo
                        .catch { error -> marcarErrorEnPublicos(error) }
                        .collect { publicos ->
                            _estadoVisual.update { estado ->
                                estado.copy(senderosPublicos = publicos, hayErrorEnPublicos = false, estaCargando = false)
                            }
                        }
                }
            },
            onFailure = { error -> marcarErrorEnPublicos(error) }
        )
    }

    private fun marcarErrorEnPublicos(error: Throwable) {
        Log.e(TAG, "No se pudieron cargar los senderos públicos", error)
        _estadoVisual.update { estado -> estado.copy(hayErrorEnPublicos = true, estaCargando = false) }
    }

    // Ignora el pedido si hay otro en curso y no pisa una ubicación válida con null
    private fun obtenerUbicacion() {
        if (trabajoUbicacion?.isActive == true) return
        trabajoUbicacion = viewModelScope.launch {
            proveedorUbicacion.obtenerUbicacionActual()
                .onSuccess { ubicacion ->
                    if (ubicacion != null) {
                        _estadoVisual.update { estado -> estado.copy(ubicacionActual = ubicacion) }
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "No se pudo obtener la ubicación", error)
                }
            val estado = _estadoVisual.value
            if (estado.ubicacionActual == null && estado.ordenarPorCercania) {
                emitir(SenderosEfecto.MostrarAviso("No pudimos obtener tu ubicación"))
            }
        }
    }

    private fun emitir(efecto: SenderosEfecto) {
        viewModelScope.launch { canalEfectos.send(efecto) }
    }
}
