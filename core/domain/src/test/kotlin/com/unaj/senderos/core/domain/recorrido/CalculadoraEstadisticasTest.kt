package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val UNA_HORA = 3_600_000L

// Estadísticas del usuario a partir de su historial: solo cuentan los recorridos reales de al menos 50 m
class CalculadoraEstadisticasTest {

    // Armar un recorrido con la distancia, la duración y los pasos indicados
    private fun recorrido(
        id: String,
        kilometros: Double,
        horas: Double = 1.0,
        pasos: Int? = null,
        esSimulado: Boolean = false
    ): RecorridoRealizado = crearRecorridoDePrueba(id = id, esSimulado = esSimulado).copy(
        distanciaKilometros = kilometros,
        duracionMilisegundos = (horas * UNA_HORA).toLong(),
        pasos = pasos
    )

    @Test
    fun `sin recorridos todo queda en cero`() {
        val estadisticas = CalculadoraEstadisticas.calcular(emptyList())

        assertEquals(0, estadisticas.cantidadRecorridos)
        assertEquals(0.0, estadisticas.distanciaTotalKilometros, 0.0)
        assertEquals(0L, estadisticas.tiempoTotalMilisegundos)
        assertEquals(0.0, estadisticas.velocidadPromedioKilometrosPorHora, 0.0)
        assertEquals(0.0, estadisticas.mayorDistanciaKilometros, 0.0)
        assertNull(estadisticas.pasosTotales)
        assertEquals(0, estadisticas.cantidadSimuladosIgnorados)
    }

    @Test
    fun `suma la distancia y el tiempo de los recorridos reales`() {
        val estadisticas = CalculadoraEstadisticas.calcular(
            listOf(recorrido("a", kilometros = 5.0, horas = 1.0), recorrido("b", kilometros = 10.0, horas = 2.5))
        )

        assertEquals(2, estadisticas.cantidadRecorridos)
        assertEquals(15.0, estadisticas.distanciaTotalKilometros, 1e-9)
        assertEquals((3.5 * UNA_HORA).toLong(), estadisticas.tiempoTotalMilisegundos)
    }

    @Test
    fun `los recorridos simulados no cuentan pero se informa cuantos se ignoraron`() {
        val estadisticas = CalculadoraEstadisticas.calcular(
            listOf(recorrido("real", kilometros = 4.0), recorrido("demo", kilometros = 80.0, esSimulado = true), recorrido("demo2", kilometros = 20.0, esSimulado = true))
        )

        assertEquals(1, estadisticas.cantidadRecorridos)
        assertEquals(4.0, estadisticas.distanciaTotalKilometros, 1e-9)
        assertEquals(2, estadisticas.cantidadSimuladosIgnorados)
    }

    @Test
    fun `los recorridos de menos de 50 metros no cuentan`() {
        val estadisticas = CalculadoraEstadisticas.calcular(
            listOf(recorrido("corto", kilometros = 0.049), recorrido("justo", kilometros = 0.05), recorrido("largo", kilometros = 3.0))
        )

        assertEquals(2, estadisticas.cantidadRecorridos)
        assertEquals(3.05, estadisticas.distanciaTotalKilometros, 1e-9)
    }

    @Test
    fun `la velocidad promedio es la distancia total sobre el tiempo total`() {
        val estadisticas = CalculadoraEstadisticas.calcular(
            listOf(recorrido("a", kilometros = 6.0, horas = 1.0), recorrido("b", kilometros = 6.0, horas = 3.0))
        )

        assertEquals(3.0, estadisticas.velocidadPromedioKilometrosPorHora, 1e-9)
    }

    @Test
    fun `la mayor distancia es la del recorrido mas largo`() {
        val estadisticas = CalculadoraEstadisticas.calcular(
            listOf(recorrido("a", kilometros = 5.0), recorrido("b", kilometros = 12.5), recorrido("c", kilometros = 7.0))
        )

        assertEquals(12.5, estadisticas.mayorDistanciaKilometros, 1e-9)
    }

    @Test
    fun `los pasos suman solo los recorridos que los midieron`() {
        val estadisticas = CalculadoraEstadisticas.calcular(
            listOf(recorrido("a", kilometros = 2.0, pasos = 3000), recorrido("b", kilometros = 2.0, pasos = null), recorrido("c", kilometros = 2.0, pasos = 1500))
        )

        assertEquals(4500, estadisticas.pasosTotales)
    }

    @Test
    fun `si ningun recorrido midio pasos los pasos totales quedan sin dato`() {
        val estadisticas = CalculadoraEstadisticas.calcular(listOf(recorrido("a", kilometros = 2.0, pasos = null)))

        assertNull(estadisticas.pasosTotales)
    }

    @Test
    fun `un tiempo total en cero no divide por cero`() {
        val estadisticas = CalculadoraEstadisticas.calcular(listOf(recorrido("a", kilometros = 2.0, horas = 0.0)))

        assertEquals(0.0, estadisticas.velocidadPromedioKilometrosPorHora, 0.0)
    }
}
