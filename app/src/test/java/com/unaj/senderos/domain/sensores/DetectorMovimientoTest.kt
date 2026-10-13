package com.unaj.senderos.domain.sensores

import org.junit.Assert.assertEquals
import org.junit.Test

class DetectorMovimientoTest {

    @Test
    fun `en movimiento con pasos recientes no esta detenido`() {
        assertEquals(false, DetectorMovimiento.estaDetenido(1.4f, 19_000L, 0L, 20_000L, midePasos = true))
    }

    @Test
    fun `velocidad baja con pasos hace cinco segundos no esta detenido`() {
        assertEquals(false, DetectorMovimiento.estaDetenido(0.2f, 15_000L, 0L, 20_000L, midePasos = true))
    }

    @Test
    fun `velocidad baja y diez segundos sin pasos esta detenido`() {
        assertEquals(true, DetectorMovimiento.estaDetenido(0.2f, 10_000L, 0L, 20_000L, midePasos = true))
    }

    @Test
    fun `velocidad desconocida y sin pasos esta detenido`() {
        assertEquals(true, DetectorMovimiento.estaDetenido(null, null, 0L, 15_000L, midePasos = true))
    }

    @Test
    fun `al comenzar sin pasos todavia no esta detenido`() {
        assertEquals(false, DetectorMovimiento.estaDetenido(null, null, 0L, 5_000L, midePasos = true))
    }

    @Test
    fun `el gps en movimiento prevalece sobre la falta de pasos`() {
        assertEquals(false, DetectorMovimiento.estaDetenido(1.0f, null, 0L, 60_000L, midePasos = true))
    }

    @Test
    fun `sin sensor de pasos decide solo la velocidad`() {
        assertEquals(true, DetectorMovimiento.estaDetenido(0.1f, null, 0L, 1_000L, midePasos = false))
        assertEquals(false, DetectorMovimiento.estaDetenido(null, null, 0L, 60_000L, midePasos = false))
        assertEquals(false, DetectorMovimiento.estaDetenido(2.0f, null, 0L, 60_000L, midePasos = false))
    }

    @Test
    fun `velocidad de medio metro por segundo ya es movimiento`() {
        assertEquals(false, DetectorMovimiento.estaDetenido(0.5f, null, 0L, 60_000L, midePasos = true))
    }

    @Test
    fun `sin pasos con velocidad desconocida y senal debil esta detenido`() {
        assertEquals(true, DetectorMovimiento.estaDetenido(null, null, 0L, 30_000L, midePasos = false, senalDebil = true))
    }

    @Test
    fun `sin pasos con velocidad desconocida y senal buena no esta detenido`() {
        assertEquals(false, DetectorMovimiento.estaDetenido(null, null, 0L, 30_000L, midePasos = false, senalDebil = false))
    }

    @Test
    fun `con senal debil una velocidad conocida y alta sigue siendo movimiento`() {
        assertEquals(false, DetectorMovimiento.estaDetenido(1.4f, null, 0L, 30_000L, midePasos = false, senalDebil = true))
    }
}
