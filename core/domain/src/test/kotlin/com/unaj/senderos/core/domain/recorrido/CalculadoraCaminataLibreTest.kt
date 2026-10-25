package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Seguimiento de una caminata libre: no hay trazado que seguir, solo se registra lo que el usuario va caminando
class CalculadoraCaminataLibreTest {

    private val inicio = Coordenada(-41.13, -71.30)

    // Leer una posición a los metros indicados al este del inicio (un grado de longitud mide unos 83,9 km a esta latitud)
    private fun lectura(metrosAlEste: Double, precision: Float? = 5f, velocidad: Float? = 1.4f) = LecturaUbicacion(
        coordenada = Coordenada(inicio.latitud, inicio.longitud + metrosAlEste / 83_900.0),
        rumboGrados = 90f,
        velocidadMetrosPorSegundo = velocidad,
        precisionMetros = precision
    )

    private fun estadoNuevo(): EstadoSeguimiento = CalculadoraSeguimiento.iniciarCaminataLibre("libre-1", "Caminata libre", ahora = 1_000L)

    @Test
    fun `iniciar una caminata libre arma un estado sin trazado que seguir`() {
        val estado = estadoNuevo()

        assertTrue(estado.esCaminataLibre)
        assertEquals("libre-1", estado.idSendero)
        assertEquals("Caminata libre", estado.nombreSendero)
        assertTrue(estado.trazado.isEmpty())
        assertEquals(1_000L, estado.instanteInicioMilisegundos)
        assertEquals(0.0, estado.distanciaRecorridaKilometros, 0.0)
    }

    @Test
    fun `la primera lectura entra al trazado caminado y marca la senal como buena`() {
        val resultado = CalculadoraSeguimiento.actualizar(estadoNuevo(), lectura(0.0), ahora = 2_000L)

        assertEquals(1, resultado.estado.trazadoCaminado.size)
        assertNotNull(resultado.estado.posicionActual)
        assertEquals(CalidadSenal.BUENA, resultado.estado.calidadSenal)
        assertEquals(1_000L, resultado.estado.tiempoTranscurridoMilisegundos)
    }

    @Test
    fun `la distancia recorrida suma los desplazamientos de 5 metros o mas`() {
        var estado = CalculadoraSeguimiento.actualizar(estadoNuevo(), lectura(0.0), ahora = 2_000L).estado
        estado = CalculadoraSeguimiento.actualizar(estado, lectura(20.0), ahora = 3_000L).estado
        estado = CalculadoraSeguimiento.actualizar(estado, lectura(40.0), ahora = 4_000L).estado

        assertEquals(0.040, estado.distanciaRecorridaKilometros, 0.002)
        assertEquals(3, estado.trazadoCaminado.size)
    }

    @Test
    fun `un movimiento de menos de 5 metros no suma distancia ni agrega punto`() {
        var estado = CalculadoraSeguimiento.actualizar(estadoNuevo(), lectura(0.0), ahora = 2_000L).estado
        estado = CalculadoraSeguimiento.actualizar(estado, lectura(2.0), ahora = 3_000L).estado

        assertEquals(0.0, estado.distanciaRecorridaKilometros, 1e-9)
        assertEquals(1, estado.trazadoCaminado.size)
    }

    @Test
    fun `una lectura imprecisa no suma distancia ni mueve la posicion`() {
        var estado = CalculadoraSeguimiento.actualizar(estadoNuevo(), lectura(0.0), ahora = 2_000L).estado
        val posicionAntes = estado.posicionActual
        estado = CalculadoraSeguimiento.actualizar(estado, lectura(300.0, precision = 80f), ahora = 3_000L).estado

        assertEquals(0.0, estado.distanciaRecorridaKilometros, 1e-9)
        assertEquals(posicionAntes, estado.posicionActual)
    }

    @Test
    fun `guarda la velocidad actual y la maxima`() {
        var estado = CalculadoraSeguimiento.actualizar(estadoNuevo(), lectura(0.0, velocidad = 1.0f), ahora = 2_000L).estado
        estado = CalculadoraSeguimiento.actualizar(estado, lectura(20.0, velocidad = 2.5f), ahora = 3_000L).estado
        estado = CalculadoraSeguimiento.actualizar(estado, lectura(40.0, velocidad = 1.5f), ahora = 4_000L).estado

        assertEquals(1.5f, estado.velocidadMetrosPorSegundo ?: 0f, 0f)
        assertEquals(2.5f, estado.velocidadMaximaMetrosPorSegundo, 0f)
    }

    @Test
    fun `nunca avisa de desvio ni de llegada porque no hay trazado`() {
        var resultado = CalculadoraSeguimiento.actualizar(estadoNuevo(), lectura(0.0), ahora = 2_000L)
        resultado = CalculadoraSeguimiento.actualizar(resultado.estado, lectura(500.0), ahora = 3_000L)

        assertTrue(resultado.avisos.isEmpty())
        assertFalse(resultado.estado.estaDesviado)
        assertFalse(resultado.estado.llegoAlFinal)
    }

    @Test
    fun `cerrar el trazado agrega la ultima posicion conocida que no habia entrado`() {
        var estado = CalculadoraSeguimiento.actualizar(estadoNuevo(), lectura(0.0), ahora = 2_000L).estado
        estado = CalculadoraSeguimiento.actualizar(estado, lectura(3.0), ahora = 3_000L).estado

        val cerrado = CalculadoraSeguimiento.cerrarTrazadoCaminado(estado)

        assertEquals(2, cerrado.size)
    }
}
