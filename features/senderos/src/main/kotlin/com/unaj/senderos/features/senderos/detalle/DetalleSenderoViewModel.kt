package com.unaj.senderos.presentation.detalle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "DETALLE_SENDERO"
private const val TAG_VIDA = "VIDA"
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

    // Indica que se confirmó la eliminación, para ignorar el null posterior del flujo
    private var estaEliminando = false

    // Trabajo de la alternancia de favorito en curso, para ignorar toques repetidos
    private var trabajoAlternarFavorito: Job? = null

    init {
        Log.d(TAG_VIDA, "ViewModel → creado (${hashCode()})")
        observarSendero()
    }

    // Registrar la destrucción del ViewModel en el Logcat
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG_VIDA, "ViewModel → onCleared")
    }

    // Procesar los eventos emitidos por la pantalla de detalle
    fun procesarEvento(evento: DetalleSenderoEvento) {
        Log.d(TAG, "Evento recibido: $evento")
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

    // Observar el sendero y reflejar cada emisión o fallo en el estado visual
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

    // Reflejar el sendero emitido o, si es null y no se está eliminando, un error de no encontrado
    private fun reflejarSendero(senderoEmitido: Sendero?) {
        if (senderoEmitido != null) {
            Log.d(TAG, "Sendero cargado: ${senderoEmitido.id}")
            _estadoVisual.update { estadoActual ->
                estadoActual.copy(sendero = senderoEmitido, estaCargando = false, mensajeError = null)
            }
        } else if (!estaEliminando) {
            mostrarError("No se encontró el sendero")
        }
    }

    // Exponer un mensaje de error terminando la carga; los mensajes vacíos usan el valor por defecto
    private fun mostrarError(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        Log.e(TAG, "Error en el detalle del sendero: $mensaje")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(estaCargando = false, mensajeError = mensaje)
        }
    }

    // Alternar el favorito mediante el caso de uso y avisar si falla
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

    // Eliminar el sendero, volver al listado si sale bien o avisar si falla
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

    // Emitir el texto para compartir el sendero cargado; no hace nada si no hay sendero
    private fun compartir() {
        val sendero = _estadoVisual.value.sendero ?: return
        val nombreDificultad = when (sendero.dificultad) {
            Dificultad.BAJA -> "Baja"
            Dificultad.MEDIA -> "Media"
            Dificultad.ALTA -> "Alta"
        }
        val texto = "${sendero.nombre} (${sendero.region})\n" +
            "Distancia: ${sendero.distanciaKilometros.roundToInt()} km · " +
            "Desnivel: ${sendero.desnivelMetros} m · Dificultad: $nombreDificultad\n" +
            "Compartido desde SenderosArgentina"
        emitirEfecto(DetalleSenderoEfecto.CompartirSendero(texto))
    }

    // Agregar o quitar una sección del conjunto de secciones expandidas
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

    // Emitir el aviso de funcionalidad todavía no disponible
    private fun emitirAvisoNoDisponible() {
        emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_NO_DISPONIBLE))
    }

    // Enviar un efecto único a la pantalla
    private fun emitirEfecto(efecto: DetalleSenderoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
