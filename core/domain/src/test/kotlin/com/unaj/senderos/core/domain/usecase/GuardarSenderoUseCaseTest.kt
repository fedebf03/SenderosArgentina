package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.SenderoInvalidoException
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardarSenderoUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")
    private val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    private val falsoSendero = FalsoSenderoRepositorio()

    private fun crearCasoDeUso(
        autenticacion: FalsoAutenticacionRepositorio = falsoAutenticacion
    ): GuardarSenderoUseCase {
        return GuardarSenderoUseCase(
            senderoRepositorio = falsoSendero,
            autenticacionRepositorio = autenticacion,
            generarIdentificador = { "id-generado" }
        )
    }

    private fun camposInvalidosDe(resultado: Result<*>): Set<CampoSendero> {
        val excepcion = resultado.exceptionOrNull()
        assertTrue(excepcion is SenderoInvalidoException)
        return (excepcion as SenderoInvalidoException).camposInvalidos
    }

    @Test
    fun `sin sesion activa falla con mensaje y no toca el repositorio`() = runTest {
        val casoDeUso = crearCasoDeUso(FalsoAutenticacionRepositorio(usuarioActual = null))

        val resultado = casoDeUso(crearSenderoDePrueba())

        assertTrue(resultado.exceptionOrNull() is IllegalStateException)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    @Test
    fun `nombre en blanco marca el campo NOMBRE`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(nombre = "   "))

        assertEquals(setOf(CampoSendero.NOMBRE), camposInvalidosDe(resultado))
    }

    @Test
    fun `distancia menor o igual a cero marca el campo DISTANCIA`() = runTest {
        val resultadoCero = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = 0.0))
        val resultadoNegativo = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = -2.5))

        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoCero))
        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoNegativo))
    }

    @Test
    fun `altura maxima menor que la minima marca el campo ALTURA_MAXIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = 1500, alturaMaximaMetros = 1000)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MAXIMA), camposInvalidosDe(resultado))
    }

    @Test
    fun `genera el identificador con el generador inyectado si esta en blanco`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(id = "")).getOrThrow()

        assertEquals("id-generado", guardado.id)
        assertEquals("id-generado", falsoSendero.llamadasGuardarSendero.single().second.id)
    }

    @Test
    fun `fuerza el idUsuario del usuario autenticado`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(idUsuario = "otro-usuario")).getOrThrow()

        assertEquals("usuario-1", guardado.idUsuario)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().first)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().second.idUsuario)
    }

    @Test
    fun `no llama al repositorio si el sendero es invalido`() = runTest {
        crearCasoDeUso()(crearSenderoDePrueba(nombre = ""))

        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    @Test
    fun `latitud fuera de rango marca el campo RECORRIDO`() = runTest {
        val resultadoNorte = crearCasoDeUso()(
            crearSenderoDePrueba(recorrido = listOf(PuntoRecorrido(latitud = 90.1, longitud = 0.0)))
        )
        val resultadoSur = crearCasoDeUso()(
            crearSenderoDePrueba(recorrido = listOf(PuntoRecorrido(latitud = -90.1, longitud = 0.0)))
        )

        assertEquals(setOf(CampoSendero.RECORRIDO), camposInvalidosDe(resultadoNorte))
        assertEquals(setOf(CampoSendero.RECORRIDO), camposInvalidosDe(resultadoSur))
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    @Test
    fun `guardar conserva el bioma del sendero`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(bioma = Bioma.YUNGAS))

        assertTrue(resultado.isSuccess)
        assertEquals(Bioma.YUNGAS, falsoSendero.llamadasGuardarSendero.last().second.bioma)
    }
}
