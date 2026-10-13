package com.unaj.senderos.presentation.revisionpuntos

import android.util.Log
import com.unaj.senderos.domain.model.PuntoCapturadoConDistancia
import com.unaj.senderos.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.presentation.comun.describirErrorParaElUsuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "REVISION_PUNTOS"
private const val MENSAJE_ERROR_POR_DEFECTO = "No se pudieron actualizar los puntos"
private const val MENSAJE_PUNTOS_AGREGADOS = "Puntos agregados al sendero"
private const val MENSAJE_PUNTOS_DESCARTADOS = "Puntos descartados"

// Cada punto parte marcado salvo que supere la distancia recomendada; idsAlternados guarda los que el usuario cambió respecto de ese valor inicial
data class RevisionPuntosEstadoVisual(
    val puntosPendientes: List<PuntoCapturadoConDistancia> = emptyList(),
    val idsAlternados: Set<String> = emptySet(),
    val esRevisionVisible: Boolean = false,
    val esConfirmarDescartarTodosVisible: Boolean = false,
    val estaProcesando: Boolean = false
) {
    val idsMarcados: Set<String>
        get() = puntosPendientes
            .filter { punto -> !punto.superaDistanciaRecomendada != (punto.punto.id in idsAlternados) }
            .map { punto -> punto.punto.id }
            .toSet()

    val idsTodos: Set<String>
        get() = puntosPendientes.map { punto -> punto.punto.id }.toSet()

    val hayPuntosPorRevisar: Boolean
        get() = puntosPendientes.isNotEmpty()
}

sealed interface RevisionPuntosEvento {
    data object AlAbrir : RevisionPuntosEvento
    data object AlCerrar : RevisionPuntosEvento
    data class AlAlternarPunto(val id: String) : RevisionPuntosEvento
    data object AlAgregarAlSendero : RevisionPuntosEvento
    data object AlTocarDescartarTodos : RevisionPuntosEvento
    data object AlConfirmarDescartarTodos : RevisionPuntosEvento
    data object AlCancelarDescartarTodos : RevisionPuntosEvento
}

// La comparten el resumen del recorrido y el detalle
class ControladorRevisionPuntos(
    private val idSendero: String,
    observarPuntosCapturadosConDistanciaUseCase: ObservarPuntosCapturadosConDistanciaUseCase,
    private val incorporarPuntosCapturadosUseCase: IncorporarPuntosCapturadosUseCase,
    private val descartarPuntosCapturadosUseCase: DescartarPuntosCapturadosUseCase,
    private val alcance: CoroutineScope,
    private val alEmitirAviso: (String) -> Unit
) {
    private val _estado = MutableStateFlow(RevisionPuntosEstadoVisual())
    val estado: StateFlow<RevisionPuntosEstadoVisual> = _estado.asStateFlow()

    init {
        observarPuntosCapturadosConDistanciaUseCase(idSendero).fold(
            onSuccess = { flujoPuntos ->
                alcance.launch {
                    flujoPuntos
                        .catch { error -> Log.e(TAG, "Error al observar los puntos capturados", error) }
                        .collect { puntos -> reflejarPuntos(puntos) }
                }
            },
            onFailure = { error -> Log.e(TAG, "No se pudieron observar los puntos capturados", error) }
        )
    }

    fun procesarEvento(evento: RevisionPuntosEvento) {
        when (evento) {
            is RevisionPuntosEvento.AlAbrir -> {
                _estado.update { estadoActual -> estadoActual.copy(esRevisionVisible = estadoActual.hayPuntosPorRevisar) }
            }
            is RevisionPuntosEvento.AlCerrar -> {
                _estado.update { estadoActual ->
                    estadoActual.copy(esRevisionVisible = false, esConfirmarDescartarTodosVisible = false)
                }
            }
            is RevisionPuntosEvento.AlAlternarPunto -> alternarPunto(evento.id)
            is RevisionPuntosEvento.AlAgregarAlSendero -> agregarAlSendero()
            is RevisionPuntosEvento.AlTocarDescartarTodos -> {
                _estado.update { estadoActual ->
                    estadoActual.copy(esConfirmarDescartarTodosVisible = estadoActual.hayPuntosPorRevisar)
                }
            }
            is RevisionPuntosEvento.AlCancelarDescartarTodos -> {
                _estado.update { estadoActual -> estadoActual.copy(esConfirmarDescartarTodosVisible = false) }
            }
            is RevisionPuntosEvento.AlConfirmarDescartarTodos -> descartarTodos()
        }
    }

    private fun reflejarPuntos(puntos: List<PuntoCapturadoConDistancia>) {
        _estado.update { estadoActual ->
            val idsVigentes = puntos.map { punto -> punto.punto.id }.toSet()
            estadoActual.copy(
                puntosPendientes = puntos,
                idsAlternados = estadoActual.idsAlternados.intersect(idsVigentes),
                esRevisionVisible = estadoActual.esRevisionVisible && puntos.isNotEmpty(),
                esConfirmarDescartarTodosVisible = estadoActual.esConfirmarDescartarTodosVisible && puntos.isNotEmpty()
            )
        }
    }

    private fun alternarPunto(id: String) {
        _estado.update { estadoActual ->
            val alternadosNuevos = if (id in estadoActual.idsAlternados) {
                estadoActual.idsAlternados - id
            } else {
                estadoActual.idsAlternados + id
            }
            estadoActual.copy(idsAlternados = alternadosNuevos)
        }
    }

    private fun agregarAlSendero() {
        val estadoActual = _estado.value
        if (estadoActual.estaProcesando || !estadoActual.hayPuntosPorRevisar) return
        val idsMarcados = estadoActual.idsMarcados
        val idsDesmarcados = estadoActual.idsTodos - idsMarcados
        _estado.update { estadoNuevo -> estadoNuevo.copy(estaProcesando = true) }
        Log.d(TAG, "Incorporando ${idsMarcados.size} puntos al sendero $idSendero y descartando ${idsDesmarcados.size}")
        alcance.launch {
            var falla: Throwable? = null
            if (idsMarcados.isNotEmpty()) {
                incorporarPuntosCapturadosUseCase(idSendero, idsMarcados).onFailure { error -> falla = error }
            }
            if (falla == null && idsDesmarcados.isNotEmpty()) {
                descartarPuntosCapturadosUseCase(idsDesmarcados).onFailure { error -> falla = error }
            }
            terminarProcesamiento(falla, if (idsMarcados.isEmpty()) MENSAJE_PUNTOS_DESCARTADOS else MENSAJE_PUNTOS_AGREGADOS)
        }
    }

    // Se invoca tras la confirmación del usuario
    private fun descartarTodos() {
        val estadoActual = _estado.value
        if (estadoActual.estaProcesando) return
        val todosLosIds = estadoActual.idsTodos
        _estado.update { estadoNuevo ->
            estadoNuevo.copy(esConfirmarDescartarTodosVisible = false, estaProcesando = todosLosIds.isNotEmpty())
        }
        if (todosLosIds.isEmpty()) return
        Log.d(TAG, "Descartando ${todosLosIds.size} puntos del sendero $idSendero")
        alcance.launch {
            val falla = descartarPuntosCapturadosUseCase(todosLosIds).exceptionOrNull()
            terminarProcesamiento(falla, MENSAJE_PUNTOS_DESCARTADOS)
        }
    }

    private fun terminarProcesamiento(falla: Throwable?, mensajeExito: String) {
        _estado.update { estadoActual ->
            estadoActual.copy(estaProcesando = false, esRevisionVisible = estadoActual.esRevisionVisible && falla != null)
        }
        if (falla == null) {
            alEmitirAviso(mensajeExito)
        } else {
            Log.e(TAG, "No se pudo completar la revisión de puntos", falla)
            alEmitirAviso(describirErrorParaElUsuario(falla, MENSAJE_ERROR_POR_DEFECTO))
        }
    }
}
