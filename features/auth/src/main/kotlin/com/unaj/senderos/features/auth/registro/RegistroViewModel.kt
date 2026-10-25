package com.unaj.senderos.features.auth.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.usecase.RegistrarUsuarioUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val registrarUsuarioUseCase: RegistrarUsuarioUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(RegistroEstadoVisual())
    val estadoVisual: StateFlow<RegistroEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<RegistroEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    fun procesarEvento(evento: RegistroEvento) {
        when (evento) {
            is RegistroEvento.AlCambiarEmail ->
                _estadoVisual.update { it.copy(email = evento.email, mensajeError = null) }
            is RegistroEvento.AlCambiarClave ->
                _estadoVisual.update { it.copy(clave = evento.clave, mensajeError = null) }
            is RegistroEvento.AlCambiarConfirmacion ->
                _estadoVisual.update { it.copy(confirmacion = evento.confirmacion, mensajeError = null) }
            is RegistroEvento.AlAlternarVisibilidadClave ->
                _estadoVisual.update { it.copy(esClaveVisible = !it.esClaveVisible) }
            is RegistroEvento.AlRegistrarse -> ejecutarRegistro()
        }
    }

    // RF1 - Registro: creamos la cuenta con correo y contraseña y, si sale bien, se entra directo al inicio
    private fun ejecutarRegistro() {
        val estadoActual = _estadoVisual.value
        _estadoVisual.update { it.copy(estaCargando = true, mensajeError = null) }

        viewModelScope.launch {
            val resultado = registrarUsuarioUseCase(
                estadoActual.email,
                estadoActual.clave,
                estadoActual.confirmacion
            )
            resultado.fold(
                onSuccess = {
                    _estadoVisual.value = RegistroEstadoVisual()
                    canalEfectos.send(RegistroEfecto.NavegarAInicio)
                },
                onFailure = { error ->
                    _estadoVisual.update {
                        it.copy(estaCargando = false, mensajeError = error.message.orEmpty())
                    }
                }
            )
        }
    }
}
