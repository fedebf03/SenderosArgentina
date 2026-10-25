package com.unaj.senderos.core.ui.clima

import org.junit.Assert.assertEquals
import org.junit.Test

// Textos de la sección de clima: temperatura, viento, antigüedad del pronóstico y nombre de cada día
class TextosClimaTest {

    @Test
    fun `la temperatura se redondea a grados enteros con la unidad`() {
        assertEquals("12 °C", formatearTemperaturaClima(12.4))
        assertEquals("13 °C", formatearTemperaturaClima(12.5))
        assertEquals("0 °C", formatearTemperaturaClima(0.2))
        assertEquals("-4 °C", formatearTemperaturaClima(-3.6))
    }

    @Test
    fun `el viento se muestra con sus rafagas si se conocen`() {
        assertEquals("Viento 22 km/h, ráfagas 41 km/h", describirVientoClima(22.0, 41.4))
        assertEquals("Viento 22 km/h", describirVientoClima(22.0, null))
    }

    @Test
    fun `un viento calmo se muestra como calma`() {
        assertEquals("Calma", describirVientoClima(0.4, null))
    }

    @Test
    fun `la antiguedad del pronostico se expresa en minutos horas o dias`() {
        assertEquals("recién", describirAntiguedadClima(20_000L))
        assertEquals("hace 1 min", describirAntiguedadClima(60_000L))
        assertEquals("hace 25 min", describirAntiguedadClima(25 * 60_000L))
        assertEquals("hace 1 h", describirAntiguedadClima(60 * 60_000L))
        assertEquals("hace 5 h", describirAntiguedadClima(5 * 3_600_000L + 40 * 60_000L))
        assertEquals("hace 1 día", describirAntiguedadClima(24 * 3_600_000L))
        assertEquals("hace 3 días", describirAntiguedadClima(3 * 24 * 3_600_000L + 5_000L))
    }

    @Test
    fun `una antiguedad negativa no rompe`() {
        assertEquals("recién", describirAntiguedadClima(-5_000L))
    }

    @Test
    fun `cada dia se nombra con su dia de la semana y su numero`() {
        assertEquals("lun 19", describirDiaClima("2026-10-19", hoy = "2026-10-17"))
        assertEquals("mar 20", describirDiaClima("2026-10-20", hoy = "2026-10-17"))
        assertEquals("dom 25", describirDiaClima("2026-10-25", hoy = "2026-10-17"))
    }

    @Test
    fun `el dia de hoy y el de manana se nombran por su nombre`() {
        assertEquals("Hoy", describirDiaClima("2026-10-18", hoy = "2026-10-18"))
        assertEquals("Mañana", describirDiaClima("2026-10-19", hoy = "2026-10-18"))
    }

    @Test
    fun `una fecha que no se puede leer se muestra tal cual`() {
        assertEquals("sin fecha", describirDiaClima("sin fecha", hoy = "2026-10-18"))
    }

    @Test
    fun `la probabilidad de lluvia se muestra como porcentaje o queda vacia`() {
        assertEquals("60 %", describirProbabilidadLluvia(60))
        assertEquals("0 %", describirProbabilidadLluvia(0))
        assertEquals("", describirProbabilidadLluvia(null))
    }

    @Test
    fun `si el pronostico guardado es de hace instantes el aviso no dice recien`() {
        assertEquals("Sin conexión: mostrando el último pronóstico guardado", describirAvisoPronosticoViejo(20_000L))
    }

    @Test
    fun `el aviso de datos viejos explica que no hay conexion y cuanto tiempo pasó`() {
        assertEquals(
            "Sin conexión: pronóstico guardado hace 3 h",
            describirAvisoPronosticoViejo(3 * 3_600_000L)
        )
    }
}
