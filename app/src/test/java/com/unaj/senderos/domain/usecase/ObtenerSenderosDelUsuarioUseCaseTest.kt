package com.unaj.senderos.domain.usecase

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

class ObtenerSenderosDelUsuarioUseCaseTest {

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
        val casoDeUso = ObtenerSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso()

        assertTrue(resultado.isFailure)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.idsUsuarioConsultados.isEmpty())
    }

    // Probar que con sesión activa devuelve los senderos del usuario consultando por su id
    @Test
    fun `con sesion activa devuelve los senderos del usuario`() = runTest {
        val senderosEsperados = listOf(
            crearSendero("1", usuario.id),
            crearSendero("2", usuario.id)
        )
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(Result.success(senderosEsperados))
        val casoDeUso = ObtenerSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso()

        assertEquals(SenderosDelUsuario(usuario, senderosEsperados), resultado.getOrNull())
        assertEquals(listOf(usuario.id), falsoSendero.idsUsuarioConsultados)
    }

    // Probar que descarta los senderos que pertenecen a otro usuario
    @Test
    fun `descarta los senderos de otro usuario`() = runTest {
        val senderoPropio = crearSendero("1", usuario.id)
        val senderoAjeno = crearSendero("2", "usuario-2")
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(Result.success(listOf(senderoPropio, senderoAjeno)))
        val casoDeUso = ObtenerSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso()

        assertEquals(SenderosDelUsuario(usuario, listOf(senderoPropio)), resultado.getOrNull())
    }

    // Probar que propaga el fallo del repositorio sin modificarlo
    @Test
    fun `propaga el fallo del repositorio`() = runTest {
        val errorEsperado = IllegalStateException("Fallo de red")
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio(Result.failure(errorEsperado))
        val casoDeUso = ObtenerSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso()

        assertTrue(resultado.isFailure)
        assertEquals("Fallo de red", resultado.exceptionOrNull()?.message)
    }
}
