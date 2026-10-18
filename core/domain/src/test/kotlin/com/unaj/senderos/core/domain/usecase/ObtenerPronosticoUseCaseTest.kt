package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.ErrorPronosticoException
import com.unaj.senderos.core.domain.port.SinConexionPronosticoException
import com.unaj.senderos.core.testing.FalsaFuentePronostico
import com.unaj.senderos.core.testing.FalsoAlmacenPronosticos
import com.unaj.senderos.core.testing.crearPronosticoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val UNA_HORA = 3_600_000L

// Pronóstico del lugar con caché: sin señal se muestra el último guardado y se avisa qué tan viejo es
@OptIn(ExperimentalCoroutinesApi::class)
class ObtenerPronosticoUseCaseTest {

    private val fuente = FalsaFuentePronostico()
    private val almacen = FalsoAlmacenPronosticos()
    private var ahora = 10 * UNA_HORA
    private val casoDeUso = ObtenerPronosticoUseCase(fuente, almacen) { ahora }
    private val lugar = Coordenada(-41.13456, -71.30123)

    @Test
    fun `sin copia guardada pide el pronostico a la red y lo guarda`() = runTest {
        fuente.resultado = Result.success(crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora))

        val resultado = casoDeUso(lugar).getOrThrow()

        assertEquals(1, fuente.lugaresPedidos.size)
        assertFalse(resultado.estaDesactualizado)
        assertEquals(null, resultado.mensajeError)
        assertEquals(1, almacen.guardados.size)
    }

    @Test
    fun `un pronostico guardado hace menos de 30 minutos se usa sin llamar a la red`() = runTest {
        val clave = ObtenerPronosticoUseCase.claveDe(lugar)
        almacen.guardados[clave] = crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora - 10 * 60_000L)

        val resultado = casoDeUso(lugar).getOrThrow()

        assertTrue(fuente.lugaresPedidos.isEmpty())
        assertFalse(resultado.estaDesactualizado)
        assertEquals(10 * 60_000L, resultado.antiguedadMilisegundos)
    }

    @Test
    fun `un pronostico guardado hace mas de 30 minutos se renueva con la red`() = runTest {
        val clave = ObtenerPronosticoUseCase.claveDe(lugar)
        almacen.guardados[clave] = crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora - UNA_HORA)
        fuente.resultado = Result.success(crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora))

        val resultado = casoDeUso(lugar).getOrThrow()

        assertEquals(1, fuente.lugaresPedidos.size)
        assertEquals(0L, resultado.antiguedadMilisegundos)
        assertEquals(ahora, almacen.guardados[clave]?.obtenidoMilisegundos)
    }

    @Test
    fun `forzar la actualizacion ignora el pronostico reciente`() = runTest {
        val clave = ObtenerPronosticoUseCase.claveDe(lugar)
        almacen.guardados[clave] = crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora - 60_000L)
        fuente.resultado = Result.success(crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora))

        casoDeUso(lugar, forzar = true)

        assertEquals(1, fuente.lugaresPedidos.size)
    }

    @Test
    fun `sin conexion y con un pronostico guardado se muestra el guardado marcado como viejo`() = runTest {
        val clave = ObtenerPronosticoUseCase.claveDe(lugar)
        almacen.guardados[clave] = crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora - 5 * UNA_HORA)
        fuente.resultado = Result.failure(SinConexionPronosticoException(null))

        val resultado = casoDeUso(lugar).getOrThrow()

        assertTrue(resultado.estaDesactualizado)
        assertEquals(5 * UNA_HORA, resultado.antiguedadMilisegundos)
        assertEquals("Sin conexión para consultar el clima", resultado.mensajeError)
    }

    @Test
    fun `sin conexion y sin nada guardado falla con el mensaje del servicio`() = runTest {
        fuente.resultado = Result.failure(SinConexionPronosticoException(null))

        val resultado = casoDeUso(lugar)

        assertTrue(resultado.isFailure)
        assertEquals("Sin conexión para consultar el clima", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `un error del servicio tambien cae al pronostico guardado`() = runTest {
        val clave = ObtenerPronosticoUseCase.claveDe(lugar)
        almacen.guardados[clave] = crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora - 2 * UNA_HORA)
        fuente.resultado = Result.failure(ErrorPronosticoException("El servicio del clima no está disponible (código 503)"))

        val resultado = casoDeUso(lugar).getOrThrow()

        assertTrue(resultado.estaDesactualizado)
        assertEquals("El servicio del clima no está disponible (código 503)", resultado.mensajeError)
    }

    @Test
    fun `un fallo al guardar la copia no impide mostrar el pronostico`() = runTest {
        almacen.resultadoGuardar = Result.failure(IllegalStateException("Sin espacio"))
        fuente.resultado = Result.success(crearPronosticoDePrueba(lugar = lugar, obtenidoMilisegundos = ahora))

        assertTrue(casoDeUso(lugar).isSuccess)
    }

    @Test
    fun `lugares a menos de un kilometro comparten la misma clave`() {
        val cerca = Coordenada(-41.1349, -71.3009)
        val lejos = Coordenada(-41.20, -71.30)

        assertEquals(ObtenerPronosticoUseCase.claveDe(lugar), ObtenerPronosticoUseCase.claveDe(cerca))
        assertTrue(ObtenerPronosticoUseCase.claveDe(lugar) != ObtenerPronosticoUseCase.claveDe(lejos))
    }

    @Test
    fun `la clave sirve como nombre de archivo`() {
        val clave = ObtenerPronosticoUseCase.claveDe(Coordenada(-41.13, -71.30))

        assertTrue(clave.matches(Regex("[a-z0-9_-]+")))
    }
}
