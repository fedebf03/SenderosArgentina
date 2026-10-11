package com.unaj.senderos.presentation.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AUTH"

class LoginViewModel(
    private val iniciarSesionUseCase: IniciarSesionUseCase,
    private val iniciarSesionConGoogleUseCase: IniciarSesionConGoogleUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(LoginEstadoVisual())
    val estadoVisual: StateFlow<LoginEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<LoginEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    fun procesarEvento(evento: LoginEvento) {
        when (evento) {
            is LoginEvento.AlCambiarEmail -> actualizarEmail(evento.email)
            is LoginEvento.AlCambiarClave -> actualizarClave(evento.clave)
            is LoginEvento.AlAlternarVisibilidadClave -> alternarVisibilidadClave()
            is LoginEvento.AlIniciarSesion -> ejecutarInicioSesion()
            is LoginEvento.AlOlvidarClave -> manejarOlvidarClave()
            is LoginEvento.AlSolicitarGoogleSignIn -> solicitarGoogleSignIn()
            is LoginEvento.AlContinuarConGoogle -> ejecutarInicioSesionConGoogle(evento.idToken)
            is LoginEvento.AlIrARegistro -> manejarIrARegistro()
        }
    }

    private fun actualizarEmail(nuevoEmail: String) {
        _estadoVisual.update { it.copy(email = nuevoEmail, mensajeError = null) }
    }

    private fun actualizarClave(nuevaClave: String) {
        _estadoVisual.update { it.copy(clave = nuevaClave, mensajeError = null) }
    }

    private fun alternarVisibilidadClave() {
        _estadoVisual.update { it.copy(esClaveVisible = !it.esClaveVisible) }
    }

    private fun ejecutarInicioSesion() {
        val estadoActual = _estadoVisual.value
        _estadoVisual.update { it.copy(estaCargando = true, mensajeError = null) }

        viewModelScope.launch {
            val resultado = iniciarSesionUseCase(estadoActual.email, estadoActual.clave)
            resultado.fold(
                onSuccess = {
                    _estadoVisual.value = LoginEstadoVisual()
                    canalEfectos.send(LoginEfecto.NavegarAInicio)
                },
                onFailure = { error ->
                    val mensaje = error.message.orEmpty()
                    Log.e(TAG, "Error en autenticación: $mensaje")
                    _estadoVisual.update { it.copy(estaCargando = false, mensajeError = mensaje) }
                }
            )
        }
    }

    private fun solicitarGoogleSignIn() {
        viewModelScope.launch {
            canalEfectos.send(LoginEfecto.SolicitarGoogleSignIn)
        }
    }

    private fun ejecutarInicioSesionConGoogle(idToken: String) {
        _estadoVisual.update { it.copy(estaCargando = true, mensajeError = null) }

        viewModelScope.launch {
            val resultado = iniciarSesionConGoogleUseCase(idToken)
            resultado.fold(
                onSuccess = {
                    _estadoVisual.value = LoginEstadoVisual()
                    canalEfectos.send(LoginEfecto.NavegarAInicio)
                },
                onFailure = { error ->
                    val mensaje = error.message.orEmpty()
                    Log.e(TAG, "Error en autenticación con Google: $mensaje")
                    _estadoVisual.update { it.copy(estaCargando = false, mensajeError = mensaje) }
                }
            )
        }
    }

    private fun manejarOlvidarClave() {
        viewModelScope.launch {
            canalEfectos.send(LoginEfecto.MostrarAviso("Todavía no disponible"))
        }
    }

    private fun manejarIrARegistro() {
        viewModelScope.launch {
            canalEfectos.send(LoginEfecto.NavegarARegistro)
        }
    }
}
