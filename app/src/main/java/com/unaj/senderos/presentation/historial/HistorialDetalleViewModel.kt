package com.unaj.senderos.presentation.historial

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarRecorridoUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "HISTORIAL"
private const val MENSAJE_NO_ENCONTRADO = "No se encontró el recorrido"
private const val MENSAJE_ERROR_POR_DEFECTO = "Ocurrió un error inesperado"

// Mostrar un recorrido terminado junto con el sendero planificado, si todavía existe
class HistorialDetalleViewModel(
    private val idRecorrido: String,
    private val observarRecorridoUseCase: ObservarRecorridoUseCase,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(HistorialDetalleEstadoVisual())
    val estadoVisual: StateFlow<HistorialDetalleEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<HistorialDetalleEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private var estaObservandoSendero = false

    init {
        Log.d(TAG, "ViewModel → creado (${hashCode()})")
        observarRecorrido()
    }

    // Registrar la destrucción del ViewModel
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel → onCleared")
    }

    // Procesar cada evento de la pantalla del detalle del historial
    fun procesarEvento(evento: HistorialDetalleEvento) {
        when (evento) {
            is HistorialDetalleEvento.AlVolver -> {
                viewModelScope.launch { canalEfectos.send(HistorialDetalleEfecto.VolverAlPerfil) }
            }
        }
    }

    // Observar el recorrido y reflejar sus cambios en el estado visual
    private fun observarRecorrido() {
        observarRecorridoUseCase(idRecorrido).fold(
            onSuccess = { flujoRecorrido ->
                viewModelScope.launch {
                    flujoRecorrido
                        .catch { error -> mostrarError(error.message) }
                        .collect { recorrido -> reflejarRecorrido(recorrido) }
                }
            },
            onFailure = { error -> mostrarError(error.message) }
        )
    }

    // Guardar el recorrido emitido o informar que no existe; al recibir el primero, observar su sendero
    private fun reflejarRecorrido(recorrido: RecorridoRealizado?) {
        if (recorrido == null) {
            mostrarError(MENSAJE_NO_ENCONTRADO)
            return
        }
        Log.d(TAG, "Recorrido ${recorrido.id} cargado")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(recorrido = recorrido, estaCargando = false, mensajeError = null)
        }
        if (!estaObservandoSendero) {
            estaObservandoSendero = true
            observarSendero(recorrido.idSendero)
        }
    }

    // Observar el sendero planificado; si ya no existe queda nulo y se muestra solo el trazado caminado
    private fun observarSendero(idSendero: String) {
        observarDetalleSenderoUseCase(idSendero).fold(
            onSuccess = { flujoSendero ->
                viewModelScope.launch {
                    flujoSendero
                        .catch { error -> Log.e(TAG, "Error al leer el sendero planificado", error) }
                        .collect { sendero -> reflejarSendero(sendero) }
                }
            },
            onFailure = { error -> Log.e(TAG, "No se pudo observar el sendero planificado", error) }
        )
    }

    // Guardar el sendero planificado emitido, o nulo si ya no existe
    private fun reflejarSendero(sendero: Sendero?) {
        _estadoVisual.update { estadoActual -> estadoActual.copy(sendero = sendero) }
    }

    // Registrar el error y mostrarlo en el estado visual
    private fun mostrarError(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        Log.e(TAG, "Error en el detalle del historial: $mensaje")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(recorrido = null, estaCargando = false, mensajeError = mensaje)
        }
    }
}
