package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PuntoCardinal
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraRumboTest {

    @Test
    fun `los centros de cada sector dan su punto cardinal`() {
        val esperados = listOf(
            0f to PuntoCardinal.NORTE, 45f to PuntoCardinal.NORESTE, 90f to PuntoCardinal.ESTE, 135f to PuntoCardinal.SURESTE,
            180f to PuntoCardinal.SUR, 225f to PuntoCardinal.SUROESTE, 270f to PuntoCardinal.OESTE, 315f to PuntoCardinal.NOROESTE
        )
        esperados.forEach { (grados, punto) -> assertEquals("Grados $grados", punto, CalculadoraRumbo.aPuntoCardinal(grados)) }
    }

    // 22,5 grados ya pertenece al sector siguiente
    @Test
    fun `los bordes entre sectores`() {
        assertEquals(PuntoCardinal.NORTE, CalculadoraRumbo.aPuntoCardinal(22.4f))
        assertEquals(PuntoCardinal.NORESTE, CalculadoraRumbo.aPuntoCardinal(22.5f))
        assertEquals(PuntoCardinal.NORESTE, CalculadoraRumbo.aPuntoCardinal(67.4f))
        assertEquals(PuntoCardinal.ESTE, CalculadoraRumbo.aPuntoCardinal(67.5f))
        assertEquals(PuntoCardinal.NOROESTE, CalculadoraRumbo.aPuntoCardinal(337.4f))
        assertEquals(PuntoCardinal.NORTE, CalculadoraRumbo.aPuntoCardinal(337.5f))
        assertEquals(PuntoCardinal.NORTE, CalculadoraRumbo.aPuntoCardinal(359.9f))
    }

    @Test
    fun `los grados fuera de rango se normalizan`() {
        assertEquals(PuntoCardinal.NORTE, CalculadoraRumbo.aPuntoCardinal(360f))
        assertEquals(PuntoCardinal.OESTE, CalculadoraRumbo.aPuntoCardinal(-90f))
        assertEquals(PuntoCardinal.ESTE, CalculadoraRumbo.aPuntoCardinal(450f))
    }

    @Test
    fun `la diferencia angular usa el camino corto`() {
        assertEquals(20f, CalculadoraRumbo.diferenciaAngularGrados(350f, 10f), 0.001f)
        assertEquals(20f, CalculadoraRumbo.diferenciaAngularGrados(10f, 350f), 0.001f)
        assertEquals(180f, CalculadoraRumbo.diferenciaAngularGrados(0f, 180f), 0.001f)
        assertEquals(0f, CalculadoraRumbo.diferenciaAngularGrados(90f, 90f), 0.001f)
    }

    @Test
    fun `cada punto cardinal tiene su simbolo corto`() {
        val simbolos = PuntoCardinal.entries.map { punto -> punto.simbolo }

        assertEquals(listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO"), simbolos)
    }
}
