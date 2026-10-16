package com.unaj.senderos.features.explorar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.usecase.GuardarCopiaSenderoPublicoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderoPublicoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.ui.comun.describirErrorParaElUsuario
import com.unaj.senderos.core.ui.comun.observarSenderos
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

private const val TAG = "DETALLE_PUBLICO"
private const val MENSAJE_ERROR = "No se pudo cargar el sendero"
private const val MENSAJE_NO_ENCONTRADO = "Este sendero ya no está publicado"
private const val MENSAJE_ERROR_AL_GUARDAR = "No se pudo guardar el sendero"

@HiltViewModel(assistedFactory = DetallePublicoViewModel.Fabrica::class)
class DetallePublicoViewModel @AssistedInject constructor(
    @Assisted private val idSendero: String,
    observarSenderoPublicoUseCase: ObservarSenderoPublicoUseCase,
    observarSenderosDelUsuarioUseCase: ObservarSenderosDelUsuarioUseCase,
    private val obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase,
    private val guardarCopiaSenderoPublicoUseCase: GuardarCopiaSenderoPublicoUseCase
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(idSendero: String): DetallePublicoViewModel
    }

    private val _estadoVisual = MutableStateFlow(DetallePublicoEstadoVisual())
    val estadoVisual: StateFlow<DetallePublicoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<DetallePublicoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    init {
        observarSendero(observarSenderoPublicoUseCase)
        observarCopia(observarSenderosDelUsuarioUseCase)
    }

    fun procesarEvento(evento: DetallePublicoEvento) {
        when (evento) {
            is DetallePublicoEvento.AlVolver -> emitir(DetallePublicoEfecto.VolverAExplorar)
            is DetallePublicoEvento.AlGuardarEnMisSenderos -> guardarEnMisSenderos()
            is DetallePublicoEvento.AlVerMiCopia -> {
                _estadoVisual.value.idCopia?.let { idCopia -> emitir(DetallePublicoEfecto.NavegarAMiSendero(idCopia)) }
            }
        }
    }

    private fun observarSendero(casoDeUso: ObservarSenderoPublicoUseCase) {
        casoDeUso(idSendero).fold(
            onSuccess = { flujo ->
                viewModelScope.launch {
                    flujo
                        .catch { error -> mostrarError(error, MENSAJE_ERROR) }
                        .collect { publico ->
                            if (publico == null) {
                                _estadoVisual.update { estado ->
                                    estado.copy(senderoPublico = null, estaCargando = false, mensajeError = MENSAJE_NO_ENCONTRADO)
                                }
                            } else {
                                val esPropio = publico.idAutor == obtenerUsuarioActualUseCase()?.id
                                _estadoVisual.update { estado ->
                                    estado.copy(
                                        senderoPublico = publico,
                                        esPropio = esPropio,
                                        estaCargando = false,
                                        mensajeError = null
                                    )
                                }
                            }
                        }
                }
            },
            onFailure = { error -> mostrarError(error, MENSAJE_ERROR) }
        )
    }

    private fun observarCopia(casoDeUso: ObservarSenderosDelUsuarioUseCase) {
        observarSenderos(
            casoDeUso,
            alRecibir = { datos ->
                val copia = datos.senderos.firstOrNull { sendero -> sendero.idOrigen == idSendero }
                _estadoVisual.update { estado -> estado.copy(idCopia = copia?.id) }
            },
            alFallar = { mensaje -> Log.e(TAG, "No se pudieron leer los senderos propios: $mensaje") }
        )
    }

    private fun guardarEnMisSenderos() {
        val estado = _estadoVisual.value
        val publico = estado.senderoPublico ?: return
        if (estado.estaGuardando || estado.esPropio || estado.idCopia != null) return
        _estadoVisual.update { actual -> actual.copy(estaGuardando = true) }
        viewModelScope.launch {
            guardarCopiaSenderoPublicoUseCase(publico).fold(
                onSuccess = { copia ->
                    _estadoVisual.update { actual -> actual.copy(estaGuardando = false) }
                    canalEfectos.send(DetallePublicoEfecto.NavegarAMiSendero(copia.id))
                },
                onFailure = { error ->
                    Log.e(TAG, "Error al guardar la copia del sendero público $idSendero", error)
                    _estadoVisual.update { actual -> actual.copy(estaGuardando = false) }
                    canalEfectos.send(
                        DetallePublicoEfecto.MostrarAviso(describirErrorParaElUsuario(error, MENSAJE_ERROR_AL_GUARDAR))
                    )
                }
            )
        }
    }

    private fun mostrarError(error: Throwable, mensaje: String) {
        Log.e(TAG, "Error en el detalle del sendero público $idSendero", error)
        _estadoVisual.update { estado -> estado.copy(estaCargando = false, mensajeError = mensaje) }
    }

    private fun emitir(efecto: DetallePublicoEfecto) {
        viewModelScope.launch { canalEfectos.send(efecto) }
    }
}
