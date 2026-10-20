package com.unaj.senderos.features.perfil.historial

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.usecase.EliminarRecorridoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarRecorridoUseCase
import kotlinx.coroutines.CancellationException
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

private const val TAG = "HISTORIAL"
private const val MENSAJE_NO_ENCONTRADO = "No se encontró el recorrido"
private const val MENSAJE_ERROR_POR_DEFECTO = "Ocurrió un error inesperado"
private const val MENSAJE_ERROR_AL_ELIMINAR = "No se pudo eliminar el recorrido. Probá de nuevo"

@HiltViewModel(assistedFactory = HistorialDetalleViewModel.Fabrica::class)
class HistorialDetalleViewModel @AssistedInject constructor(
    @Assisted private val idRecorrido: String,
    private val observarRecorridoUseCase: ObservarRecorridoUseCase,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val eliminarRecorridoUseCase: EliminarRecorridoUseCase
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(idRecorrido: String): HistorialDetalleViewModel
    }

    private val _estadoVisual = MutableStateFlow(HistorialDetalleEstadoVisual())
    val estadoVisual: StateFlow<HistorialDetalleEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<HistorialDetalleEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private var estaObservandoSendero = false

    init {
        Log.d(TAG, "ViewModel → creado (${hashCode()})")
        observarRecorrido()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel → onCleared")
    }

    fun procesarEvento(evento: HistorialDetalleEvento) {
        when (evento) {
            is HistorialDetalleEvento.AlVolver -> {
                viewModelScope.launch { canalEfectos.send(HistorialDetalleEfecto.VolverAlPerfil) }
            }
            is HistorialDetalleEvento.AlConvertirEnSendero -> convertirEnSendero()
            is HistorialDetalleEvento.AlTocarEliminar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarVisible = true, errorAlEliminar = null) }
            }
            is HistorialDetalleEvento.AlCancelarEliminar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarVisible = false) }
            }
            is HistorialDetalleEvento.AlConfirmarEliminar -> eliminar()
        }
    }

    private fun eliminar() {
        if (_estadoVisual.value.estaEliminando) return
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaEliminando = true, esDialogoEliminarVisible = false, errorAlEliminar = null) }
        viewModelScope.launch {
            val resultado = try {
                eliminarRecorridoUseCase(idRecorrido)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            resultado.fold(
                onSuccess = {
                    Log.d(TAG, "Recorrido $idRecorrido eliminado")
                    canalEfectos.send(HistorialDetalleEfecto.VolverAlPerfil)
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo eliminar el recorrido $idRecorrido", error)
                    _estadoVisual.update { estadoActual ->
                        estadoActual.copy(estaEliminando = false, errorAlEliminar = MENSAJE_ERROR_AL_ELIMINAR)
                    }
                }
            )
        }
    }

    private fun convertirEnSendero() {
        val estado = _estadoVisual.value
        val recorrido = estado.recorrido ?: return
        if (!estado.sePuedeConvertirEnSendero) return
        viewModelScope.launch { canalEfectos.send(HistorialDetalleEfecto.NavegarAFormularioDesdeCaminata(recorrido.idSendero)) }
    }

    // RF5 - Historial: observamos el recorrido realizado y, si salió de un sendero, también su trazado
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

    private fun reflejarRecorrido(recorrido: RecorridoRealizado?) {
        if (recorrido == null) {
            mostrarError(MENSAJE_NO_ENCONTRADO)
            return
        }
        Log.d(TAG, "Recorrido ${recorrido.id} cargado")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(recorrido = recorrido, estaCargando = false, mensajeError = null)
        }
        if (!estaObservandoSendero && !IdentificadorCaminataLibre.esDeCaminataLibre(recorrido.idSendero)) {
            estaObservandoSendero = true
            observarSendero(recorrido.idSendero)
        }
    }

    // Si el sendero planificado ya no existe queda nulo y se muestra solo el trazado caminado
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

    private fun reflejarSendero(sendero: Sendero?) {
        _estadoVisual.update { estadoActual -> estadoActual.copy(sendero = sendero) }
    }

    private fun mostrarError(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        Log.e(TAG, "Error en el detalle del historial: $mensaje")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(recorrido = null, estaCargando = false, mensajeError = mensaje)
        }
    }
}
