package com.unaj.senderos.domain.usecase

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservarDetalleSenderoUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")

    private fun crearSendero(id: String, idUsuario: String): Sendero {
        return Sendero(
            id = id,
            idUsuario = idUsuario,
            nombre = "Sendero $id",
            region = "Patagonia",
            distanciaKilometros = 10.0,
            dificultad = Dificultad.MEDIA
        )
    }

    @Test
    fun `sin sesion activa falla con mensaje y no consulta el repositorio`() = runTest {
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = null)
        val falsoSendero = FalsoSenderoRepositorio()
        val casoDeUso = ObservarDetalleSenderoUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso("1")

        assertTrue(resultado.isFailure)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.consultasSendero.isEmpty())
    }

    @Test
    fun `con sesion activa delega con el id del usuario y emite el sendero`() = runTest {
        val sendero = crearSendero("1", usuario.id)
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(listOf(sendero))
        val casoDeUso = ObservarDetalleSenderoUseCase(falsoSendero, falsoAutenticacion)

        casoDeUso("1").getOrThrow().test {
            assertEquals(sendero, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(usuario.id to "1"), falsoSendero.consultasSendero)
    }

    @Test
    fun `emite null para un sendero de otro usuario`() = runTest {
        val senderoAjeno = crearSendero("1", "usuario-2")
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(listOf(senderoAjeno))
        val casoDeUso = ObservarDetalleSenderoUseCase(falsoSendero, falsoAutenticacion)

        casoDeUso("1").getOrThrow().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emite null cuando el sendero es eliminado de la fuente`() = runTest {
        val sendero = crearSendero("1", usuario.id)
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(listOf(sendero))
        val casoDeUso = ObservarDetalleSenderoUseCase(falsoSendero, falsoAutenticacion)

        casoDeUso("1").getOrThrow().test {
            assertEquals(sendero, awaitItem())
            falsoSendero.senderos.value = emptyList()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
