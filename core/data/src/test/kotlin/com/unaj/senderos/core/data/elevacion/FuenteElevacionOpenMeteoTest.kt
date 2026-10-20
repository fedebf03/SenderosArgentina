package com.unaj.senderos.core.data.elevacion

import com.unaj.senderos.core.network.openmeteo.ApiElevacionOpenMeteo
import com.unaj.senderos.core.network.openmeteo.RespuestaElevacionOpenMeteo
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.ErrorElevacionException
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class FuenteElevacionOpenMeteoTest {

    private class FalsaApi : ApiElevacionOpenMeteo {
        val pedidos = mutableListOf<Pair<String, String>>()
        var alResponder: (cantidad: Int) -> RespuestaElevacionOpenMeteo = { cantidad ->
            RespuestaElevacionOpenMeteo(List(cantidad) { indice -> indice.toDouble() })
        }
        var excepcion: Throwable? = null

        override suspend fun obtenerElevaciones(latitudes: String, longitudes: String): RespuestaElevacionOpenMeteo {
            pedidos.add(latitudes to longitudes)
            excepcion?.let { error -> throw error }
            return alResponder(latitudes.split(",").size)
        }
    }

    private val api = FalsaApi()
    private val fuente = FuenteElevacionOpenMeteo(api)

    private fun coordenadas(cantidad: Int) = List(cantidad) { indice -> Coordenada(-40.0 - indice * 0.001, -70.0 + indice * 0.001) }

    @Test
    fun `un pedido chico formatea las listas separadas por coma con cinco decimales`() = runTest {
        val resultado = fuente.obtenerAltitudes(listOf(Coordenada(-40.5, -70.25), Coordenada(-41.123456, -71.0)))

        assertEquals(listOf(0.0, 1.0), resultado.getOrThrow())
        assertEquals(listOf("-40.50000,-41.12346" to "-70.25000,-71.00000"), api.pedidos)
    }

    @Test
    fun `una lista vacia no llama a la red`() = runTest {
        val resultado = fuente.obtenerAltitudes(emptyList())

        assertTrue(resultado.getOrThrow().isEmpty())
        assertTrue(api.pedidos.isEmpty())
    }

    @Test
    fun `mas de cien coordenadas se trocean en lotes de cien conservando el orden`() = runTest {
        val lista = coordenadas(250)
        var contador = 0.0
        api.alResponder = { cantidad -> RespuestaElevacionOpenMeteo(List(cantidad) { contador++ }) }

        val altitudes = fuente.obtenerAltitudes(lista).getOrThrow()

        assertEquals(listOf(100, 100, 50), api.pedidos.map { pedido -> pedido.first.split(",").size })
        assertEquals(250, altitudes.size)
        assertEquals((0 until 250).map { indice -> indice.toDouble() }, altitudes)
    }

    @Test
    fun `una falla de conectividad se informa como sin conexion`() = runTest {
        api.excepcion = IOException("sin red")

        val error = fuente.obtenerAltitudes(coordenadas(3)).exceptionOrNull()

        assertTrue(error is SinConexionElevacionException)
        assertEquals("Sin conexión para calcular el perfil", error?.message)
    }

    @Test
    fun `un tiempo de espera agotado tambien se informa como sin conexion`() = runTest {
        api.excepcion = SocketTimeoutException("lento")

        val error = fuente.obtenerAltitudes(coordenadas(3)).exceptionOrNull()

        assertTrue(error is SinConexionElevacionException)
    }

    private fun httpException(codigo: Int) = HttpException(Response.error<RespuestaElevacionOpenMeteo>(codigo, "".toResponseBody()))

    @Test
    fun `los codigos http se informan con mensajes en espanol`() = runTest {
        api.excepcion = httpException(429)
        val demasiados = fuente.obtenerAltitudes(coordenadas(3)).exceptionOrNull()
        api.excepcion = httpException(400)
        val rechazado = fuente.obtenerAltitudes(coordenadas(3)).exceptionOrNull()
        api.excepcion = httpException(503)
        val caido = fuente.obtenerAltitudes(coordenadas(3)).exceptionOrNull()

        assertEquals("El servicio de elevación recibió demasiados pedidos, probá más tarde", demasiados?.message)
        assertEquals("El servicio de elevación rechazó el pedido (código 400)", rechazado?.message)
        assertEquals("El servicio de elevación no está disponible (código 503)", caido?.message)
        assertTrue(demasiados is ErrorElevacionException && demasiados !is SinConexionElevacionException)
        assertTrue((demasiados as ErrorElevacionException).esLimitacionDelServicio)
        assertTrue((caido as ErrorElevacionException).esLimitacionDelServicio)
        assertTrue((rechazado as ErrorElevacionException).esRechazoDefinitivo)
        assertTrue(!rechazado.esLimitacionDelServicio && !demasiados.esRechazoDefinitivo && !caido.esRechazoDefinitivo)
    }

    @Test
    fun `una respuesta con otra cantidad de alturas es un error de datos incompletos`() = runTest {
        api.alResponder = { RespuestaElevacionOpenMeteo(listOf(1.0)) }

        val error = fuente.obtenerAltitudes(coordenadas(3)).exceptionOrNull()

        assertEquals("El servicio de elevación devolvió datos incompletos", error?.message)
    }

    @Test
    fun `una respuesta que no se puede interpretar es un error de respuesta invalida`() = runTest {
        api.excepcion = SerializationException("json roto")

        val error = fuente.obtenerAltitudes(coordenadas(3)).exceptionOrNull()

        assertEquals("El servicio de elevación devolvió una respuesta inválida", error?.message)
    }

    @Test
    fun `la cancelacion se relanza y no se convierte en failure`() = runTest {
        api.excepcion = CancellationException("cancelado")

        try {
            fuente.obtenerAltitudes(coordenadas(3))
            fail("Debía relanzar la cancelación")
        } catch (excepcion: CancellationException) {
            assertEquals("cancelado", excepcion.message)
        }
    }
}
