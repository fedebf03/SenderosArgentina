package com.unaj.senderos.domain.recorrido

import com.unaj.senderos.domain.model.EstadoSeguimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EstadoSeguimientoSensoresTest {

    private val base = EstadoSeguimiento(idSendero = "s", trazado = emptyList())

    @Test
    fun `detenido con brujula usa el rumbo de la brujula`() {
        val estado = base.copy(estaDetenido = true, rumboBrujulaGrados = 200f, rumboGrados = 90f)

        assertEquals(200f, estado.rumboEfectivoGrados)
    }

    @Test
    fun `en movimiento usa el rumbo del gps`() {
        val estado = base.copy(estaDetenido = false, rumboBrujulaGrados = 200f, rumboGrados = 90f)

        assertEquals(90f, estado.rumboEfectivoGrados)
    }

    @Test
    fun `detenido sin brujula usa el ultimo rumbo del gps`() {
        val estado = base.copy(estaDetenido = true, rumboBrujulaGrados = null, rumboGrados = 90f)

        assertEquals(90f, estado.rumboEfectivoGrados)
    }

    @Test
    fun `sin rumbos el efectivo es null`() {
        assertNull(base.rumboEfectivoGrados)
    }

    @Test
    fun `la velocidad promedio es distancia sobre tiempo`() {
        val estado = base.copy(distanciaRecorridaKilometros = 3.0, tiempoTranscurridoMilisegundos = 1_800_000L)

        assertEquals(6.0, estado.velocidadPromedioKilometrosPorHora, 0.0001)
    }

    @Test
    fun `sin tiempo la velocidad promedio es cero`() {
        assertEquals(0.0, base.velocidadPromedioKilometrosPorHora, 0.0001)
    }
}
