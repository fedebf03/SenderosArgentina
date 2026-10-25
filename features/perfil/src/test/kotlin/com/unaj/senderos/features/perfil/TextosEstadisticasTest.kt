package com.unaj.senderos.features.perfil

import org.junit.Assert.assertEquals
import org.junit.Test

private const val UN_MINUTO = 60_000L
private const val UNA_HORA = 3_600_000L

// Textos de las estadísticas del perfil
class TextosEstadisticasTest {

    @Test
    fun `el tiempo total se expresa en minutos u horas`() {
        assertEquals("0 min", describirTiempoTotal(0L))
        assertEquals("45 min", describirTiempoTotal(45 * UN_MINUTO))
        assertEquals("1 h 30 min", describirTiempoTotal(90 * UN_MINUTO))
        assertEquals("3 h", describirTiempoTotal(3 * UNA_HORA))
        assertEquals("125 h 20 min", describirTiempoTotal(125 * UNA_HORA + 20 * UN_MINUTO))
    }

    @Test
    fun `los segundos sueltos no se muestran`() {
        assertEquals("0 min", describirTiempoTotal(59_000L))
        assertEquals("1 min", describirTiempoTotal(61_000L))
    }

    @Test
    fun `el aviso de simulados dice cuantos recorridos de demostracion no cuentan`() {
        assertEquals("", describirSimuladosIgnorados(0))
        assertEquals("No incluye 1 recorrido simulado", describirSimuladosIgnorados(1))
        assertEquals("No incluye 3 recorridos simulados", describirSimuladosIgnorados(3))
    }
}
