package com.unaj.senderos.core.data.clima

import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.ErrorPronosticoException
import com.unaj.senderos.core.domain.port.SinConexionPronosticoException
import com.unaj.senderos.core.network.openmeteo.ApiPronosticoOpenMeteo
import com.unaj.senderos.core.network.openmeteo.ClimaActualOpenMeteo
import com.unaj.senderos.core.network.openmeteo.ClimaDiarioOpenMeteo
import com.unaj.senderos.core.network.openmeteo.RespuestaPronosticoOpenMeteo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

// Fuente del pronóstico que consulta Open-Meteo: traduce la respuesta al dominio y los fallos a mensajes en español
class FuentePronosticoOpenMeteoTest {

    private class FalsaApi : ApiPronosticoOpenMeteo {
        val pedidos = mutableListOf<Pair<Double, Double>>()
        var respuesta: RespuestaPronosticoOpenMeteo = respuestaCompleta()
        var excepcion: Throwable? = null

        override suspend fun obtenerPronostico(latitud: Double, longitud: Double): RespuestaPronosticoOpenMeteo {
            pedidos.add(latitud to longitud)
            excepcion?.let { error -> throw error }
            return respuesta
        }
    }

    private companion object {
        // Respuesta de dos días como la que devuelve la API
        fun respuestaCompleta() = RespuestaPronosticoOpenMeteo(
            actual = ClimaActualOpenMeteo(
                temperatura = 11.4,
                sensacionTermica = 8.9,
                viento = 22.0,
                rafagas = 41.5,
                codigoClima = 3,
                esDeDia = 1
            ),
            diario = ClimaDiarioOpenMeteo(
                fechas = listOf("2026-10-19", "2026-10-20"),
                codigoClima = listOf(61, 95),
                temperaturaMaxima = listOf(16.2, 14.0),
                temperaturaMinima = listOf(4.1, 2.5),
                precipitacion = listOf(3.4, 22.0),
                probabilidadLluvia = listOf(60, null),
                vientoMaximo = listOf(28.0, 44.0),
                rafagasMaximas = listOf(45.0, 70.0)
            )
        )
    }

    private val api = FalsaApi()
    private val fuente = FuentePronosticoOpenMeteo(api) { 1_700_000_000_000L }
    private val lugar = Coordenada(-41.13, -71.30)

    @Test
    fun `pide el pronostico de la coordenada indicada`() = runTest {
        fuente.obtener(lugar)

        assertEquals(listOf(-41.13 to -71.30), api.pedidos)
    }

    @Test
    fun `traduce el clima de ahora`() = runTest {
        val actual = fuente.obtener(lugar).getOrThrow().actual

        assertEquals(11.4, actual.temperaturaCelsius, 0.0)
        assertEquals(8.9, actual.sensacionTermicaCelsius ?: Double.NaN, 0.0)
        assertEquals(22.0, actual.vientoKilometrosPorHora, 0.0)
        assertEquals(41.5, actual.rafagasKilometrosPorHora ?: Double.NaN, 0.0)
        assertEquals(CondicionClima.NUBLADO, actual.condicion)
        assertTrue(actual.esDeDia)
    }

    @Test
    fun `traduce cada dia con su condicion y su probabilidad de lluvia`() = runTest {
        val dias = fuente.obtener(lugar).getOrThrow().dias

        assertEquals(2, dias.size)
        assertEquals("2026-10-19", dias[0].fecha)
        assertEquals(CondicionClima.LLUVIA, dias[0].condicion)
        assertEquals(16.2, dias[0].temperaturaMaximaCelsius, 0.0)
        assertEquals(4.1, dias[0].temperaturaMinimaCelsius, 0.0)
        assertEquals(60, dias[0].probabilidadLluviaPorcentaje)
        assertEquals(3.4, dias[0].precipitacionMilimetros, 0.0)
        assertEquals(CondicionClima.TORMENTA, dias[1].condicion)
        assertEquals(70.0, dias[1].rafagasMaximasKilometrosPorHora, 0.0)
    }

    @Test
    fun `una probabilidad de lluvia ausente queda desconocida`() = runTest {
        val dias = fuente.obtener(lugar).getOrThrow().dias

        assertNull(dias[1].probabilidadLluviaPorcentaje)
    }

    @Test
    fun `guarda el lugar pedido y el instante en que se obtuvo`() = runTest {
        val pronostico = fuente.obtener(lugar).getOrThrow()

        assertEquals(lugar, pronostico.lugar)
        assertEquals(1_700_000_000_000L, pronostico.obtenidoMilisegundos)
    }

    @Test
    fun `las rafagas y la sensacion termica ausentes quedan desconocidas`() = runTest {
        api.respuesta = respuestaCompleta().let { completa ->
            completa.copy(actual = completa.actual?.copy(rafagas = null, sensacionTermica = null))
        }

        val actual = fuente.obtener(lugar).getOrThrow().actual

        assertNull(actual.rafagasKilometrosPorHora)
        assertNull(actual.sensacionTermicaCelsius)
    }

    @Test
    fun `un valor numerico ausente en un dia se trata como cero para no inventar avisos`() = runTest {
        api.respuesta = respuestaCompleta().let { completa ->
            completa.copy(diario = completa.diario?.copy(precipitacion = listOf(null, 2.0), rafagasMaximas = listOf(null, 10.0)))
        }

        val dia = fuente.obtener(lugar).getOrThrow().dias.first()

        assertEquals(0.0, dia.precipitacionMilimetros, 0.0)
        assertEquals(0.0, dia.rafagasMaximasKilometrosPorHora, 0.0)
    }

    @Test
    fun `una respuesta sin el clima de ahora es un error de datos incompletos`() = runTest {
        api.respuesta = respuestaCompleta().copy(actual = null)

        val error = fuente.obtener(lugar).exceptionOrNull()

        assertEquals("El servicio del clima devolvió datos incompletos", error?.message)
    }

    @Test
    fun `una respuesta con listas de distinto largo es un error de datos incompletos`() = runTest {
        api.respuesta = respuestaCompleta().let { completa ->
            completa.copy(diario = completa.diario?.copy(temperaturaMaxima = listOf(16.2)))
        }

        val error = fuente.obtener(lugar).exceptionOrNull()

        assertEquals("El servicio del clima devolvió datos incompletos", error?.message)
    }

    @Test
    fun `una falla de conectividad se informa como sin conexion`() = runTest {
        api.excepcion = IOException("sin red")

        val error = fuente.obtener(lugar).exceptionOrNull()

        assertTrue(error is SinConexionPronosticoException)
        assertEquals("Sin conexión para consultar el clima", error?.message)
    }

    @Test
    fun `un tiempo de espera agotado tambien se informa como sin conexion`() = runTest {
        api.excepcion = SocketTimeoutException("lento")

        assertTrue(fuente.obtener(lugar).exceptionOrNull() is SinConexionPronosticoException)
    }

    private fun httpException(codigo: Int) = HttpException(Response.error<RespuestaPronosticoOpenMeteo>(codigo, "".toResponseBody()))

    @Test
    fun `los codigos http se informan con mensajes en espanol`() = runTest {
        api.excepcion = httpException(429)
        val demasiados = fuente.obtener(lugar).exceptionOrNull()
        api.excepcion = httpException(400)
        val rechazado = fuente.obtener(lugar).exceptionOrNull()
        api.excepcion = httpException(503)
        val caido = fuente.obtener(lugar).exceptionOrNull()

        assertEquals("El servicio del clima recibió demasiados pedidos, probá más tarde", demasiados?.message)
        assertEquals("El servicio del clima rechazó el pedido (código 400)", rechazado?.message)
        assertEquals("El servicio del clima no está disponible (código 503)", caido?.message)
        assertTrue(demasiados is ErrorPronosticoException && demasiados !is SinConexionPronosticoException)
    }

    @Test
    fun `una respuesta que no se puede leer es un error de respuesta invalida`() = runTest {
        api.excepcion = SerializationException("mal formada")

        val error = fuente.obtener(lugar).exceptionOrNull()

        assertEquals("El servicio del clima devolvió una respuesta inválida", error?.message)
    }

    @Test
    fun `una cancelacion no se atrapa`() = runTest {
        api.excepcion = CancellationException("cancelado")

        try {
            fuente.obtener(lugar)
            fail("La cancelación debía propagarse")
        } catch (excepcion: CancellationException) {
            assertEquals("cancelado", excepcion.message)
        }
    }
}
