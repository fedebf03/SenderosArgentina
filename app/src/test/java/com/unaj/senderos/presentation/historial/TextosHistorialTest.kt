package com.unaj.senderos.presentation.historial

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TextosHistorialTest {

    private val zonaArgentina = ZoneId.of("America/Argentina/Buenos_Aires")

    // Verificar el formato de fecha y hora en es-AR según la zona indicada
    @Test
    fun `la fecha y hora se formatean como dia mes anio y hora`() {
        // 2026-10-12T04:33:00Z equivale a 01:33 en Buenos Aires
        val milisegundos = 1_791_779_580_000L

        assertEquals("12/10/2026 · 01:33", formatearFechaHora(milisegundos, zonaArgentina))
    }

    // Verificar que la velocidad lleva coma decimal y la unidad
    @Test
    fun `la velocidad se muestra con coma y unidad`() {
        assertEquals("7,2 km/h", formatearVelocidad(7.2))
        assertEquals("0,0 km/h", formatearVelocidad(0.0))
    }

    // Verificar el texto de los pasos y del ritmo, con su alternativa cuando faltan
    @Test
    fun `los pasos y el ritmo nulos indican que no estan disponibles`() {
        assertEquals("No disponible en este dispositivo", formatearPasos(null))
        assertEquals("1.234", formatearPasos(1234))
        assertEquals("No disponible en este dispositivo", formatearRitmo(null))
        assertEquals("95 pasos/min", formatearRitmo(95.4))
    }
}
