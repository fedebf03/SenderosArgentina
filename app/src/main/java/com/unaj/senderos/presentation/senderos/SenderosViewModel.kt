package com.unaj.senderos.presentation.senderos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SENDEROS"
private const val TAG_VIDA = "VIDA"

class SenderosViewModel(
    private val observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(SenderosEstadoVisual())
    val estadoVisual: StateFlow<SenderosEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<SenderosEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    init {
        Log.d(TAG_VIDA, "ViewModel → creado (${hashCode()})")
        cargarSenderos()
    }

    // Registrar la destrucción del ViewModel en el Logcat
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG_VIDA, "ViewModel → onCleared")
    }

    // Procesar los eventos emitidos por la pantalla de senderos
    fun procesarEvento(evento: SenderosEvento) {
        when (evento) {
            is SenderosEvento.AlCambiarBusqueda -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(textoBusqueda = evento.texto) }
            }
            is SenderosEvento.AlSeleccionarSeccion -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(seccionSeleccionada = evento.seccion) }
            }
            is SenderosEvento.AlTocarAvatar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuPerfilVisible = true) }
            }
            is SenderosEvento.AlCerrarMenuPerfil -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuPerfilVisible = false) }
            }
            is SenderosEvento.AlCerrarSesion -> cerrarSesion()
            is SenderosEvento.AlTocarNotificaciones -> emitirAvisoNoDisponible()
            is SenderosEvento.AlTocarCrearSendero -> {
                viewModelScope.launch {
                    canalEfectos.send(SenderosEfecto.NavegarACreacion)
                }
            }
            is SenderosEvento.AlSeleccionarSendero -> {
                Log.d(TAG, "Sendero seleccionado: ${evento.idSendero}")
                viewModelScope.launch {
                    canalEfectos.send(SenderosEfecto.NavegarADetalle(evento.idSendero))
                }
            }
        }
    }

    // Observar los senderos del usuario y reflejar cada emisión o fallo en el estado visual
    private fun cargarSenderos() {
        observarSenderosDelUsuarioUseCase().fold(
            onSuccess = { flujoSenderos ->
                viewModelScope.launch {
                    flujoSenderos
                        .catch { error -> mostrarErrorDeCarga(error) }
                        .collect { senderosDelUsuario ->
                            Log.d(TAG, "Senderos cargados: ${senderosDelUsuario.senderos.size}")
                            _estadoVisual.update { estadoActual ->
                                estadoActual.copy(
                                    usuario = senderosDelUsuario.usuario,
                                    senderos = senderosDelUsuario.senderos,
                                    estaCargando = false
                                )
                            }
                        }
                }
            },
            onFailure = { error -> mostrarErrorDeCarga(error) }
        )
    }

    // Registrar el error en Logcat y exponerlo en el estado visual terminando la carga
    private fun mostrarErrorDeCarga(error: Throwable) {
        val mensaje = error.message?.takeIf { mensajeOriginal -> mensajeOriginal.isNotBlank() }
            ?: "No se pudieron cargar los senderos"
        Log.e(TAG, "Error al cargar senderos: $mensaje", error)
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(estaCargando = false, mensajeError = mensaje)
        }
    }

    // Cerrar la sesión, ocultar el menú y solicitar la navegación al login
    private fun cerrarSesion() {
        Log.d(TAG, "Cierre de sesión solicitado")
        cerrarSesionUseCase()
        _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuPerfilVisible = false) }
        viewModelScope.launch {
            canalEfectos.send(SenderosEfecto.NavegarALogin)
        }
    }

    // Emitir el aviso de funcionalidad todavía no disponible
    private fun emitirAvisoNoDisponible() {
        viewModelScope.launch {
            canalEfectos.send(SenderosEfecto.MostrarAviso("Todavía no disponible"))
        }
    }
}
