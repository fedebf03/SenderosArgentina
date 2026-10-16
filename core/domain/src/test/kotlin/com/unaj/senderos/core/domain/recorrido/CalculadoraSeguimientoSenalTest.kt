package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07
private const val INSTANTE_INICIAL = 1_000L

class CalculadoraSeguimientoSenalTest {

    private val sendero = crearSenderoDePrueba(
        recorrido = listOf(
            PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
            PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE)
        )
    ).copy(tipoRecorrido = TipoRecorrido.LINEAL)

    private fun lectura(gradosAlSur: Double, velocidad: Float?, precision: Float?): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE), null, velocidad, precision)
    }

    private fun estadoConLecturaValida(instante: Long) =
        CalculadoraSeguimiento.actualizar(
            CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL),
            lectura(0.0005, velocidad = 1.5f, precision = 5f),
            instante
        ).estado

    @Test
    fun `una lectura imprecisa toma la velocidad del GPS`() {
        val estado = estadoConLecturaValida(2_000L)

        val resultado = CalculadoraSeguimiento.actualizar(estado, lectura(0.001, velocidad = 0.1f, precision = 50f), 4_000L).estado

        assertEquals(0.1f, resultado.velocidadMetrosPorSegundo)
        assertEquals(CalidadSenal.BUENA, resultado.calidadSenal)
        assertEquals(estado.posicionActual, resultado.posicionActual)
    }

    @Test
    fun `una lectura imprecisa con senal debil deja la velocidad desconocida`() {
        val estado = estadoConLecturaValida(2_000L)

        val resultado = CalculadoraSeguimiento.actualizar(estado, lectura(0.001, velocidad = 1.5f, precision = 50f), 20_000L).estado

        assertEquals(CalidadSenal.DEBIL, resultado.calidadSenal)
        assertNull(resultado.velocidadMetrosPorSegundo)
    }

    @Test
    fun `reevaluar la senal anula la velocidad al pasar a debil`() {
        val estado = estadoConLecturaValida(2_000L)

        val debil = CalculadoraSeguimiento.reevaluarSenal(estado, 20_000L)

        assertEquals(CalidadSenal.DEBIL, debil.calidadSenal)
        assertNull(debil.velocidadMetrosPorSegundo)
    }

    @Test
    fun `reevaluar la senal buena conserva la velocidad`() {
        val estado = estadoConLecturaValida(2_000L)

        val buena = CalculadoraSeguimiento.reevaluarSenal(estado, 5_000L)

        assertEquals(CalidadSenal.BUENA, buena.calidadSenal)
        assertNotNull(buena.velocidadMetrosPorSegundo)
    }

    @Test
    fun `con el umbral por defecto una lectura de cincuenta metros se descarta`() {
        val inicial = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val resultado = CalculadoraSeguimiento.actualizar(inicial, lectura(0.001, null, 50f), 2_000L).estado

        assertNull(resultado.posicionActual)
    }

    @Test
    fun `con umbral de cien metros una lectura de cincuenta metros se acepta`() {
        val inicial = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val resultado = CalculadoraSeguimiento.actualizar(inicial, lectura(0.001, null, 50f), 2_000L, precisionMaximaMetros = 100f).estado

        assertNotNull(resultado.posicionActual)
    }

    @Test
    fun `el umbral de cien metros incluye el limite y descarta lo que lo supera`() {
        val inicial = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

        val enElLimite = CalculadoraSeguimiento.actualizar(inicial, lectura(0.001, null, 100f), 2_000L, precisionMaximaMetros = 100f).estado
        val porEncima = CalculadoraSeguimiento.actualizar(inicial, lectura(0.001, null, 100.5f), 2_000L, precisionMaximaMetros = 100f).estado

        assertNotNull(enElLimite.posicionActual)
        assertNull(porEncima.posicionActual)
    }
}
