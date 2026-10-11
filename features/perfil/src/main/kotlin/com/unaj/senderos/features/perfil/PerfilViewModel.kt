package com.unaj.senderos.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PerfilViewModel(
    obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(PerfilEstadoVisual(usuario = obtenerUsuarioActualUseCase()))
    val estadoVisual: StateFlow<PerfilEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<PerfilEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    fun procesarEvento(evento: PerfilEvento) {
        when (evento) {
            is PerfilEvento.AlCerrarSesion -> cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        cerrarSesionUseCase()
        viewModelScope.launch {
            canalEfectos.send(PerfilEfecto.NavegarALogin)
        }
    }
}
