package com.unaj.senderos.features.mapa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.port.MonitorConectividad
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosPublicosUseCase
import com.unaj.senderos.core.ui.comun.observarSenderos
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "SENDEROS_MAPA"

@HiltViewModel
class MapaSenderosViewModel @Inject constructor(
    observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase,
    observarSenderosPublicosUseCase: ObservarSenderosPublicosUseCase,
    monitorConectividad: MonitorConectividad
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(MapaSenderosEstadoVisual())
    val estadoVisual: StateFlow<MapaSenderosEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<MapaSenderosEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    // RF2 - Mapa: en el mapa se juntan los senderos propios con los públicos y se avisa cuando no hay conexión
    init {
        observarConexion(monitorConectividad)
        observarPublicos(observarSenderosPublicosUseCase)
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

    private fun observarPublicos(casoDeUso: ObservarSenderosPublicosUseCase) {
        casoDeUso().onSuccess { flujo ->
            viewModelScope.launch {
                flujo
                    .catch { error -> Log.e(TAG, "No se pudieron cargar los senderos públicos", error) }
                    .collect { publicos ->
                        _estadoVisual.update { estado -> estado.copy(senderosPublicos = publicos) }
                    }
            }
        }
    }

    private fun observarConexion(monitorConectividad: MonitorConectividad) {
        viewModelScope.launch {
            monitorConectividad.observarConexion()
                .catch { error -> Log.e(TAG, "No se pudo observar la conexión", error) }
                .collect { hayConexion ->
                    _estadoVisual.update { estado -> estado.copy(estaSinConexion = !hayConexion) }
                }
        }
    }

    fun procesarEvento(evento: MapaSenderosEvento) {
        when (evento) {
            is MapaSenderosEvento.AlSeleccionarSendero -> {
                _estadoVisual.update { estado -> estado.copy(idSeleccionado = evento.idSendero) }
            }
            is MapaSenderosEvento.AlSolicitarUbicacion -> {
                _estadoVisual.update { estado -> estado.copy(seSolicitoUbicacion = true) }
            }
            // RF2 - Mapa: un sendero público de otro usuario abre el detalle público y uno propio abre su detalle
            is MapaSenderosEvento.AlAbrirDetalle -> {
                val efecto = if (_estadoVisual.value.esPublicoDeOtro(evento.idSendero)) {
                    MapaSenderosEfecto.NavegarADetallePublico(evento.idSendero)
                } else {
                    MapaSenderosEfecto.NavegarADetalle(evento.idSendero)
                }
                viewModelScope.launch { canalEfectos.send(efecto) }
            }
        }
    }
}
