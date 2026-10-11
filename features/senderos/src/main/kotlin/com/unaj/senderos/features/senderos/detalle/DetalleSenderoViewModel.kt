package com.unaj.senderos.presentation.detalle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.presentation.componentes.obtenerTextoDificultad
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DETALLE_SENDERO"
private const val MENSAJE_ERROR_POR_DEFECTO = "Ocurrió un error inesperado"
private const val MENSAJE_NO_DISPONIBLE = "Todavía no disponible"

class DetalleSenderoViewModel(
    private val idSendero: String,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val alternarFavoritoUseCase: AlternarFavoritoUseCase,
    private val eliminarSenderoUseCase: EliminarSenderoUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(DetalleSenderoEstadoVisual())
    val estadoVisual: StateFlow<DetalleSenderoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<DetalleSenderoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private var estaEliminando = false

    private var trabajoAlternarFavorito: Job? = null

    init {
        observarSendero()
    }

    fun procesarEvento(evento: DetalleSenderoEvento) {
        when (evento) {
            is DetalleSenderoEvento.AlVolver -> emitirEfecto(DetalleSenderoEfecto.VolverAlListado)
            is DetalleSenderoEvento.AlAlternarFavorito -> alternarFavorito()
            is DetalleSenderoEvento.AlCompartir -> compartir()
            is DetalleSenderoEvento.AlAlternarSeccion -> alternarSeccion(evento.seccion)
            is DetalleSenderoEvento.AlTocarIniciarRecorrido -> emitirAvisoNoDisponible()
            is DetalleSenderoEvento.AlAbrirMenuOpciones -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuOpcionesVisible = true) }
            }
            is DetalleSenderoEvento.AlCerrarMenuOpciones -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuOpcionesVisible = false) }
            }
            is DetalleSenderoEvento.AlTocarEditar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuOpcionesVisible = false) }
                emitirEfecto(DetalleSenderoEfecto.NavegarAEdicion(idSendero))
            }
            is DetalleSenderoEvento.AlTocarEliminar -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(esMenuOpcionesVisible = false, esDialogoEliminarVisible = true)
                }
            }
            is DetalleSenderoEvento.AlCancelarEliminar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarVisible = false) }
            }
            is DetalleSenderoEvento.AlConfirmarEliminar -> eliminar()
        }
    }

    private fun observarSendero() {
        observarDetalleSenderoUseCase(idSendero).fold(
            onSuccess = { flujoSendero ->
                viewModelScope.launch {
                    flujoSendero
                        .catch { error -> mostrarError(error.message) }
                        .collect { senderoEmitido -> reflejarSendero(senderoEmitido) }
                }
            },
            onFailure = { error -> mostrarError(error.message) }
        )
    }

    private fun reflejarSendero(senderoEmitido: Sendero?) {
        if (senderoEmitido != null) {
            _estadoVisual.update { estadoActual ->
                estadoActual.copy(sendero = senderoEmitido, estaCargando = false, mensajeError = null)
            }
        } else if (!estaEliminando) {
            mostrarError("No se encontró el sendero")
        }
    }

    private fun mostrarError(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        Log.e(TAG, "Error en el detalle del sendero: $mensaje")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(estaCargando = false, mensajeError = mensaje)
        }
    }

    private fun alternarFavorito() {
        if (trabajoAlternarFavorito?.isActive == true) return
        trabajoAlternarFavorito = viewModelScope.launch {
            alternarFavoritoUseCase(idSendero).onFailure { error ->
                Log.e(TAG, "Error al alternar favorito", error)
                canalEfectos.send(
                    DetalleSenderoEfecto.MostrarAviso(error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO)
                )
            }
        }
    }

    private fun eliminar() {
        if (estaEliminando) return
        _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarVisible = false) }
        estaEliminando = true
        viewModelScope.launch {
            eliminarSenderoUseCase(idSendero).fold(
                onSuccess = { canalEfectos.send(DetalleSenderoEfecto.VolverAlListado) },
                onFailure = { error ->
                    estaEliminando = false
                    Log.e(TAG, "Error al eliminar el sendero", error)
                    canalEfectos.send(
                        DetalleSenderoEfecto.MostrarAviso(error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO)
                    )
                }
            )
        }
    }

    private fun compartir() {
        val sendero = _estadoVisual.value.sendero ?: return
        val texto = "${sendero.nombre} (${sendero.region})\n" +
            "Distancia: ${sendero.distanciaKilometros.roundToInt()} km · " +
            "Desnivel: ${sendero.desnivelMetros} m · Dificultad: ${obtenerTextoDificultad(sendero.dificultad)}\n" +
            "Compartido desde SenderosArgentina"
        emitirEfecto(DetalleSenderoEfecto.CompartirSendero(texto))
    }

    private fun alternarSeccion(seccion: SeccionDetalle) {
        _estadoVisual.update { estadoActual ->
            val seccionesNuevas = if (seccion in estadoActual.seccionesExpandidas) {
                estadoActual.seccionesExpandidas - seccion
            } else {
                estadoActual.seccionesExpandidas + seccion
            }
            estadoActual.copy(seccionesExpandidas = seccionesNuevas)
        }
    }

    private fun emitirAvisoNoDisponible() {
        emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_NO_DISPONIBLE))
    }

    private fun emitirEfecto(efecto: DetalleSenderoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
