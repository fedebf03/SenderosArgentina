package com.unaj.senderos.presentation.mapasenderos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.presentation.comun.observarSenderos
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapaSenderosViewModel(
    observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(MapaSenderosEstadoVisual())
    val estadoVisual: StateFlow<MapaSenderosEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<MapaSenderosEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    init {
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

    fun procesarEvento(evento: MapaSenderosEvento) {
        when (evento) {
            is MapaSenderosEvento.AlSeleccionarSendero -> {
                _estadoVisual.update { estado -> estado.copy(idSeleccionado = evento.idSendero) }
            }
            is MapaSenderosEvento.AlSolicitarUbicacion -> {
                _estadoVisual.update { estado -> estado.copy(seSolicitoUbicacion = true) }
            }
            is MapaSenderosEvento.AlAbrirDetalle -> {
                viewModelScope.launch { canalEfectos.send(MapaSenderosEfecto.NavegarADetalle(evento.idSendero)) }
            }
        }
    }
}
