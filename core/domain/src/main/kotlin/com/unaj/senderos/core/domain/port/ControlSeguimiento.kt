package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// Todos los métodos se invocan desde el hilo principal (el control confina su estado a Dispatchers.Main.immediate), por eso no usa sincronización adicional
interface ControlSeguimiento {

    val estado: StateFlow<EstadoSeguimiento?>

    // Motivo del último fin no pedido por el usuario (p. ej. permiso revocado); null si no hubo o si se inició otro recorrido
    val motivoFin: StateFlow<MotivoFinSeguimiento?>

    // Avisos de proximidad y desvío generados durante el recorrido activo
    val avisos: SharedFlow<AvisoRecorrido>

    // Iniciar el seguimiento de un sendero, real o simulado; finaliza el recorrido anterior si había uno
    suspend fun iniciar(idSendero: String, esSimulado: Boolean = false)

    // Cambiar la velocidad de avance de la simulación (1, 5 o 20); no hace nada si el recorrido no es simulado
    fun cambiarMultiplicadorSimulacion(valor: Int)

    // Desplazar la posición simulada fuera del trazado unos segundos; no hace nada si el recorrido no es simulado
    fun simularDesvio()

    // Detener el seguimiento activo guardando el recorrido realizado en el historial
    fun detener()

    // Cancelar el seguimiento activo descartando lo recorrido: no se guarda nada en el historial
    fun cancelar()
}
