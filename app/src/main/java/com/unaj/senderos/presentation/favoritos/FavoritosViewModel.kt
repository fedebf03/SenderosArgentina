package com.unaj.senderos.presentation.favoritos

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

class FavoritosViewModel(
    observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(FavoritosEstadoVisual())
    val estadoVisual: StateFlow<FavoritosEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<FavoritosEfecto>(Channel.BUFFERED)
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

    fun procesarEvento(evento: FavoritosEvento) {
        when (evento) {
            is FavoritosEvento.AlCambiarBusqueda -> {
                _estadoVisual.update { estado -> estado.copy(textoBusqueda = evento.texto) }
            }
            is FavoritosEvento.AlSeleccionarSendero -> {
                viewModelScope.launch { canalEfectos.send(FavoritosEfecto.NavegarADetalle(evento.idSendero)) }
            }
        }
    }
}
