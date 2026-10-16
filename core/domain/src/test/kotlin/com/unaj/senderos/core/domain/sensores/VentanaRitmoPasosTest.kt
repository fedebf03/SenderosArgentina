package com.unaj.senderos.core.domain.sensores

import org.junit.Assert.assertEquals
import org.junit.Test

class VentanaRitmoPasosTest {

    @Test
    fun `sin pasos el ritmo es cero`() {
        assertEquals(0.0, VentanaRitmoPasos().ritmoPasosPorMinuto(5_000L), 0.0001)
    }

    @Test
    fun `un solo paso da ritmo cero`() {
        val ventana = VentanaRitmoPasos()
        ventana.registrarPaso(1_000L)

        assertEquals(0.0, ventana.ritmoPasosPorMinuto(1_200L), 0.0001)
    }

    @Test
    fun `dos pasos por segundo dan ciento veinte por minuto`() {
        val ventana = VentanaRitmoPasos()
        for (indice in 0..20) ventana.registrarPaso(indice * 500L)

        assertEquals(120.0, ventana.ritmoPasosPorMinuto(10_100L), 0.5)
    }

    @Test
    fun `los pasos fuera de la ventana no cuentan`() {
        val ventana = VentanaRitmoPasos()
        for (indice in 0..10) ventana.registrarPaso(indice * 1_000L)
        for (indice in 0..10) ventana.registrarPaso(30_000L + indice * 500L)

        assertEquals(120.0, ventana.ritmoPasosPorMinuto(35_100L), 0.5)
    }

    @Test
    fun `el ritmo cae a cero si el ultimo paso es viejo`() {
        val ventana = VentanaRitmoPasos()
        for (indice in 0..10) ventana.registrarPaso(indice * 500L)

        assertEquals(0.0, ventana.ritmoPasosPorMinuto(8_000L), 0.0001)
    }

    @Test
    fun `reiniciar borra los pasos`() {
        val ventana = VentanaRitmoPasos()
        for (indice in 0..10) ventana.registrarPaso(indice * 500L)

        ventana.reiniciar()

        assertEquals(0.0, ventana.ritmoPasosPorMinuto(5_100L), 0.0001)
    }
}
