package com.unaj.senderos.presentation.senderos

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

class SenderosViewModel(
    observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(SenderosEstadoVisual())
    val estadoVisual: StateFlow<SenderosEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<SenderosEfecto>(Channel.BUFFERED)
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

    fun procesarEvento(evento: SenderosEvento) {
        when (evento) {
            is SenderosEvento.AlCambiarBusqueda -> {
                _estadoVisual.update { estado -> estado.copy(textoBusqueda = evento.texto) }
            }
            is SenderosEvento.AlTocarCrearSendero -> emitir(SenderosEfecto.NavegarACreacion)
            is SenderosEvento.AlSeleccionarSendero -> emitir(SenderosEfecto.NavegarADetalle(evento.idSendero))
        }
    }

    private fun emitir(efecto: SenderosEfecto) {
        viewModelScope.launch { canalEfectos.send(efecto) }
    }
}
