package com.unaj.senderos.domain.usecase

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.SenderosDelUsuario
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservarSenderosDelUsuarioUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")

    // Crear un sendero de prueba perteneciente al usuario indicado
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

    // Probar que sin sesión activa falla con mensaje y no consulta el repositorio
    @Test
    fun `sin sesion activa falla con mensaje y no consulta el repositorio`() = runTest {
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = null)
        val falsoSendero = FalsoSenderoRepositorio()
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso()

        assertTrue(resultado.isFailure)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.idsUsuarioConsultados.isEmpty())
    }

    // Probar que con sesión activa emite los senderos del usuario consultando por su id
    @Test
    fun `con sesion activa emite los senderos del usuario`() = runTest {
        val senderosEsperados = listOf(
            crearSendero("1", usuario.id),
            crearSendero("2", usuario.id)
        )
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(senderosEsperados)
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso()

        assertTrue(resultado.isSuccess)
        resultado.getOrThrow().test {
            assertEquals(SenderosDelUsuario(usuario, senderosEsperados), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(usuario.id), falsoSendero.idsUsuarioConsultados)
    }

    // Probar que descarta los senderos que pertenecen a otro usuario
    @Test
    fun `descarta los senderos de otro usuario`() = runTest {
        val senderoPropio = crearSendero("1", usuario.id)
        val senderoAjeno = crearSendero("2", "usuario-2")
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(listOf(senderoPropio, senderoAjeno))
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        casoDeUso().getOrThrow().test {
            assertEquals(SenderosDelUsuario(usuario, listOf(senderoPropio)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que el flujo emite de nuevo cuando la fuente falsa cambia la lista de senderos
    @Test
    fun `emite una nueva lista cuando cambia la fuente de datos`() = runTest {
        val primerSendero = crearSendero("1", usuario.id)
        val segundoSendero = crearSendero("2", usuario.id)
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(listOf(primerSendero))
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        casoDeUso().getOrThrow().test {
            assertEquals(listOf(primerSendero), awaitItem().senderos)
            falsoSendero.senderos.value = listOf(primerSendero, segundoSendero)
            assertEquals(listOf(primerSendero, segundoSendero), awaitItem().senderos)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que la sesión se lee una sola vez al invocar y no al recolectar el flujo
    @Test
    fun `lee la sesion una sola vez al invocar`() = runTest {
        val senderoPropio = crearSendero("1", usuario.id)
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(listOf(senderoPropio))
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        val flujo = casoDeUso().getOrThrow()
        falsoAutenticacion.usuarioActual = null

        flujo.test {
            assertEquals(SenderosDelUsuario(usuario, listOf(senderoPropio)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
