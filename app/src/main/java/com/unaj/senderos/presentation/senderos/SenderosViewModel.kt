package com.unaj.senderos.presentation.senderos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.port.MonitorConectividad
import com.unaj.senderos.domain.port.ProveedorUbicacion
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.presentation.comun.observarSenderos
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

private const val TAG = "SENDEROS"

class SenderosViewModel(
    observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase,
    private val proveedorUbicacion: ProveedorUbicacion,
    monitorConectividad: MonitorConectividad
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(SenderosEstadoVisual())
    val estadoVisual: StateFlow<SenderosEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<SenderosEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private var trabajoUbicacion: Job? = null

    init {
        observarConexion(monitorConectividad)
        observarSenderos(
            observarSenderosDelUsuarioUseCase,
            alRecibir = { datos ->
                _estadoVisual.update { estado ->
                    estado.copy(usuario = datos.usuario, senderos = datos.senderos, estaCargando = false)
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
            is SenderosEvento.AlObtenerUbicacion -> obtenerUbicacion()
            is SenderosEvento.AlSolicitarPermisoNotificaciones -> {
                _estadoVisual.update { estado -> estado.copy(seSolicitoPermisoNotificaciones = true) }
            }
            is SenderosEvento.AlTocarCrearSendero -> emitir(SenderosEfecto.NavegarACreacion)
            is SenderosEvento.AlSeleccionarSendero -> emitir(SenderosEfecto.NavegarADetalle(evento.idSendero))
        }
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
