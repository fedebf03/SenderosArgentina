package com.unaj.senderos.domain.recorrido

import com.unaj.senderos.domain.model.EstadoSeguimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EstadoSeguimientoSensoresTest {

    private val base = EstadoSeguimiento(idSendero = "s", trazado = emptyList())

    // Verificar que detenido y con brújula el rumbo efectivo es el de la brújula
    @Test
    fun `detenido con brujula usa el rumbo de la brujula`() {
        val estado = base.copy(estaDetenido = true, rumboBrujulaGrados = 200f, rumboGrados = 90f)

        assertEquals(200f, estado.rumboEfectivoGrados)
    }

    // Verificar que en movimiento el rumbo efectivo es el del GPS
    @Test
    fun `en movimiento usa el rumbo del gps`() {
        val estado = base.copy(estaDetenido = false, rumboBrujulaGrados = 200f, rumboGrados = 90f)

        assertEquals(90f, estado.rumboEfectivoGrados)
    }

    // Verificar que detenido sin brújula conserva el último rumbo del GPS
    @Test
    fun `detenido sin brujula usa el ultimo rumbo del gps`() {
        val estado = base.copy(estaDetenido = true, rumboBrujulaGrados = null, rumboGrados = 90f)

        assertEquals(90f, estado.rumboEfectivoGrados)
    }

    // Verificar que sin ningún rumbo el efectivo es null
    @Test
    fun `sin rumbos el efectivo es null`() {
        assertNull(base.rumboEfectivoGrados)
    }

    // Verificar la velocidad promedio como distancia sobre tiempo transcurrido
    @Test
    fun `la velocidad promedio es distancia sobre tiempo`() {
        val estado = base.copy(distanciaRecorridaKilometros = 3.0, tiempoTranscurridoMilisegundos = 1_800_000L)

        assertEquals(6.0, estado.velocidadPromedioKilometrosPorHora, 0.0001)
    }

    // Verificar que sin tiempo la velocidad promedio es cero
    @Test
    fun `sin tiempo la velocidad promedio es cero`() {
        assertEquals(0.0, base.velocidadPromedioKilometrosPorHora, 0.0001)
    }
}
