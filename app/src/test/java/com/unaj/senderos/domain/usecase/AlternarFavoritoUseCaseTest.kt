package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternarFavoritoUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")

    @Test
    fun `sin sesion activa falla con mensaje y no toca el repositorio`() = runTest {
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = null)
        val falsoSendero = FalsoSenderoRepositorio()
        val casoDeUso = AlternarFavoritoUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso("1")

        assertTrue(resultado.isFailure)
        assertEquals("No hay una sesión activa", resultado.exceptionOrNull()?.message)
        assertTrue(falsoSendero.llamadasAlternarFavorito.isEmpty())
    }

    @Test
    fun `con sesion activa delega con el id del usuario`() = runTest {
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio()
        val casoDeUso = AlternarFavoritoUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso("sendero-9")

        assertTrue(resultado.isSuccess)
        assertEquals(listOf(usuario.id to "sendero-9"), falsoSendero.llamadasAlternarFavorito)
    }

    @Test
    fun `propaga el fallo del repositorio`() = runTest {
        val falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        val falsoSendero = FalsoSenderoRepositorio()
        falsoSendero.resultadoAlternarFavorito = Result.failure(IllegalStateException("Fallo al guardar"))
        val casoDeUso = AlternarFavoritoUseCase(falsoSendero, falsoAutenticacion)

        val resultado = casoDeUso("1")

        assertTrue(resultado.isFailure)
        assertEquals("Fallo al guardar", resultado.exceptionOrNull()?.message)
    }
}
