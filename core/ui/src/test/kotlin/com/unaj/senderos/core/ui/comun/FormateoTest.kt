package com.unaj.senderos.core.ui.comun

import org.junit.Assert.assertEquals
import org.junit.Test

class FormateoTest {

    @Test
    fun `los kilometros llevan un decimal solo si no son enteros`() {
        assertEquals("12", formatearKilometros(12.0))
        assertEquals("12,5", formatearKilometros(12.5))
        assertEquals("4,5", formatearKilometros(4.5))
        assertEquals("0", formatearKilometros(0.0))
    }

    @Test
    fun `los kilometros redondeados a entero no muestran decimal`() {
        assertEquals("10", formatearKilometros(9.96))
        assertEquals("1234,5", formatearKilometros(1234.5))
    }

    @Test
    fun `la distancia de un sendero lleva la unidad`() {
        assertEquals("570 km", formatearDistanciaSendero(570.0))
        assertEquals("4,5 km", formatearDistanciaSendero(4.5))
    }

    @Test
    fun `la distancia corta se expresa en metros o kilometros`() {
        assertEquals("850 m", formatearDistanciaCorta(0.85))
        assertEquals("1,5 km", formatearDistanciaCorta(1.5))
    }

    @Test
    fun `la velocidad se formatea con coma y unidad`() {
        assertEquals("7,2 km/h", formatearVelocidad(7.2))
        assertEquals("0,0 km/h", formatearVelocidad(0.0))
        assertEquals("5,4 km/h", formatearVelocidadDesdeMetrosPorSegundo(1.5f))
        assertEquals("—", formatearVelocidadDesdeMetrosPorSegundo(null))
    }

    @Test
    fun `los pasos y el ritmo usan el texto alternativo cuando faltan`() {
        assertEquals("1.234", formatearPasos(1234))
        assertEquals("—", formatearPasos(null))
        assertEquals(TEXTO_NO_DISPONIBLE, formatearPasos(null, TEXTO_NO_DISPONIBLE))
        assertEquals("95 pasos/min", formatearRitmo(95.4))
        assertEquals("—", formatearRitmo(null))
        assertEquals(TEXTO_NO_DISPONIBLE, formatearRitmo(null, TEXTO_NO_DISPONIBLE))
    }

    @Test
    fun `el texto de no disponible habla del dispositivo`() {
        assertEquals("No disponible en este dispositivo", TEXTO_NO_DISPONIBLE)
    }

    @Test
    fun `la distancia al usuario cambia de unidad y precision`() {
        assertEquals("a 850 m de vos", formatearDistanciaAlUsuario(0.85))
        assertEquals("a 1,5 km de vos", formatearDistanciaAlUsuario(1.5))
        assertEquals("a 42 km de vos", formatearDistanciaAlUsuario(42.4))
    }

    @Test
    fun `el decimal editable usa coma y omite el decimal entero`() {
        assertEquals("12", formatearDecimalEditable(12.0))
        assertEquals("12,5", formatearDecimalEditable(12.5))
        assertEquals("1234,25", formatearDecimalEditable(1234.25))
    }
}
