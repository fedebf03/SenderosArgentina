package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.SenderoInvalidoException
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardarSenderoUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")
    private val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    private val falsoSendero = FalsoSenderoRepositorio()

    // Crear el caso de uso con sus dependencias de prueba
    private fun crearCasoDeUso(
        autenticacion: FalsoAutenticacionRepositorio = falsoAutenticacion
    ): GuardarSenderoUseCase {
        return GuardarSenderoUseCase(
            senderoRepositorio = falsoSendero,
            autenticacionRepositorio = autenticacion,
            generarIdentificador = { "id-generado" }
        )
    }

    // Obtener los campos inválidos informados en el resultado fallido
    private fun camposInvalidosDe(resultado: Result<*>): Set<CampoSendero> {
        val excepcion = resultado.exceptionOrNull()
        assertTrue(excepcion is SenderoInvalidoException)
        return (excepcion as SenderoInvalidoException).camposInvalidos
    }

    // Verificar que sin sesion activa falla con mensaje y no toca el repositorio
    @Test
    fun `sin sesion activa falla con mensaje y no toca el repositorio`() = runTest {
        val casoDeUso = crearCasoDeUso(FalsoAutenticacionRepositorio(usuarioActual = null))

        val resultado = casoDeUso(crearSenderoDePrueba())

        assertTrue(resultado.exceptionOrNull() is IllegalStateException)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    // Verificar que nombre en blanco marca el campo NOMBRE
    @Test
    fun `nombre en blanco marca el campo NOMBRE`() = runTest {
        val resultado = crearCasoDeUso()(crearSenderoDePrueba(nombre = "   "))

        assertEquals(setOf(CampoSendero.NOMBRE), camposInvalidosDe(resultado))
    }

    // Verificar que distancia menor o igual a cero marca el campo DISTANCIA
    @Test
    fun `distancia menor o igual a cero marca el campo DISTANCIA`() = runTest {
        val resultadoCero = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = 0.0))
        val resultadoNegativo = crearCasoDeUso()(crearSenderoDePrueba(distanciaKilometros = -2.5))

        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoCero))
        assertEquals(setOf(CampoSendero.DISTANCIA), camposInvalidosDe(resultadoNegativo))
    }

    // Verificar que altura maxima menor que la minima marca el campo ALTURA_MAXIMA
    @Test
    fun `altura maxima menor que la minima marca el campo ALTURA_MAXIMA`() = runTest {
        val resultado = crearCasoDeUso()(
            crearSenderoDePrueba(alturaMinimaMetros = 1500, alturaMaximaMetros = 1000)
        )

        assertEquals(setOf(CampoSendero.ALTURA_MAXIMA), camposInvalidosDe(resultado))
    }

    // Verificar que genera el identificador con el generador inyectado si esta en blanco
    @Test
    fun `genera el identificador con el generador inyectado si esta en blanco`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(id = "")).getOrThrow()

        assertEquals("id-generado", guardado.id)
        assertEquals("id-generado", falsoSendero.llamadasGuardarSendero.single().second.id)
    }

    // Verificar que fuerza el idUsuario del usuario autenticado
    @Test
    fun `fuerza el idUsuario del usuario autenticado`() = runTest {
        val guardado = crearCasoDeUso()(crearSenderoDePrueba(idUsuario = "otro-usuario")).getOrThrow()

        assertEquals("usuario-1", guardado.idUsuario)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().first)
        assertEquals("usuario-1", falsoSendero.llamadasGuardarSendero.single().second.idUsuario)
    }

    // Verificar que no llama al repositorio si el sendero es invalido
    @Test
    fun `no llama al repositorio si el sendero es invalido`() = runTest {
        crearCasoDeUso()(crearSenderoDePrueba(nombre = ""))

        assertTrue(falsoSendero.llamadasGuardarSendero.isEmpty())
    }

    // Verificar que latitud fuera de rango marca el campo RECORRIDO
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
}
