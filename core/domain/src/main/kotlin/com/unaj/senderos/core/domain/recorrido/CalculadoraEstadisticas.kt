package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.RecorridoRealizado

private const val DISTANCIA_MINIMA_KILOMETROS = 0.05
private const val MILISEGUNDOS_POR_HORA = 3_600_000.0

// Totales del usuario; pasosTotales es nulo si ningún recorrido contado midió pasos, y cantidadSimuladosIgnorados cuenta los de demostración que no entraron
data class EstadisticasUsuario(
    val cantidadRecorridos: Int,
    val distanciaTotalKilometros: Double,
    val tiempoTotalMilisegundos: Long,
    val velocidadPromedioKilometrosPorHora: Double,
    val mayorDistanciaKilometros: Double,
    val pasosTotales: Int?,
    val cantidadSimuladosIgnorados: Int
)

object CalculadoraEstadisticas {

    // Calcular las estadísticas del historial: cuentan solo los recorridos reales (no simulados) de al menos 50 m; los de menos son pruebas sin distancia
    fun calcular(recorridos: List<RecorridoRealizado>): EstadisticasUsuario {
        val reales = recorridos.filter { recorrido -> !recorrido.esSimulado && recorrido.distanciaKilometros >= DISTANCIA_MINIMA_KILOMETROS }
        val distanciaTotal = reales.sumOf { recorrido -> recorrido.distanciaKilometros }
        val tiempoTotal = reales.sumOf { recorrido -> recorrido.duracionMilisegundos }
        val horas = tiempoTotal / MILISEGUNDOS_POR_HORA
        val conPasos = reales.mapNotNull { recorrido -> recorrido.pasos }
        return EstadisticasUsuario(
            cantidadRecorridos = reales.size,
            distanciaTotalKilometros = distanciaTotal,
            tiempoTotalMilisegundos = tiempoTotal,
            velocidadPromedioKilometrosPorHora = if (horas > 0.0) distanciaTotal / horas else 0.0,
            mayorDistanciaKilometros = reales.maxOfOrNull { recorrido -> recorrido.distanciaKilometros } ?: 0.0,
            pasosTotales = if (conPasos.isEmpty()) null else conPasos.sum(),
            cantidadSimuladosIgnorados = recorridos.count { recorrido -> recorrido.esSimulado }
        )
    }
}
