package com.unaj.senderos.core.ui.comun

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FechaHoraTest {

    private val zonaArgentina = ZoneId.of("America/Argentina/Buenos_Aires")

    @Test
    fun `la fecha y hora se formatean como dia mes anio y hora`() {
        // 2026-10-12T04:33:00Z equivale a 01:33 en Buenos Aires
        val milisegundos = 1_791_779_580_000L

        assertEquals("12/10/2026 · 01:33", formatearFechaHora(milisegundos, zonaArgentina))
    }
}
