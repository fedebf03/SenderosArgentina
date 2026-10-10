package com.unaj.senderos.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderoRepositorioMockTest {

    // Probar que emite dos senderos de Huella Andina y Huella Neuquina con el idUsuario pedido
    @Test
    fun `emite dos senderos del usuario pedido`() = runTest {
        val repositorio = SenderoRepositorioMock()

        val senderos = repositorio.observarSenderosDelUsuario("usuario-7").first()

        assertEquals(2, senderos.size)
        assertEquals(
            setOf("Huella Andina", "Huella Neuquina"),
            senderos.map { sendero -> sendero.nombre }.toSet()
        )
        assertTrue(senderos.all { sendero -> sendero.idUsuario == "usuario-7" })
    }

    // Probar que observarSendero emite el sendero de ejemplo con sus datos de detalle
    @Test
    fun `observarSendero emite el sendero con sus datos de detalle`() = runTest {
        val repositorio = SenderoRepositorioMock()

        val sendero = repositorio.observarSendero("usuario-1", "huella-andina").first()

        assertNotNull(sendero)
        assertEquals("Huella Andina", sendero?.nombre)
        assertEquals(1200, sendero?.desnivelMetros)
        assertEquals(3, sendero?.etapas?.size)
        assertFalse(sendero?.esFavorito ?: true)
    }

    // Probar que alternarFavorito invierte esFavorito y el cambio se refleja en observarSendero
    @Test
    fun `alternarFavorito invierte esFavorito y se refleja en observarSendero`() = runTest {
        val repositorio = SenderoRepositorioMock()

        repositorio.observarSendero("usuario-1", "huella-andina").test {
            assertFalse(awaitItem()?.esFavorito ?: true)

            val resultado = repositorio.alternarFavorito("usuario-1", "huella-andina")

            assertTrue(resultado.isSuccess)
            assertTrue(awaitItem()?.esFavorito ?: false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que alternarFavorito se refleja también en observarSenderosDelUsuario
    @Test
    fun `alternarFavorito se refleja en observarSenderosDelUsuario`() = runTest {
        val repositorio = SenderoRepositorioMock()

        repositorio.observarSenderosDelUsuario("usuario-1").test {
            awaitItem()

            repositorio.alternarFavorito("usuario-1", "huella-andina")

            val listaActualizada = awaitItem()
            val senderoAndino = listaActualizada.first { sendero -> sendero.id == "huella-andina" }
            val senderoNeuquino = listaActualizada.first { sendero -> sendero.id == "huella-neuquina" }
            assertTrue(senderoAndino.esFavorito)
            assertFalse(senderoNeuquino.esFavorito)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Probar que alternarFavorito dos veces devuelve el sendero a no favorito
    @Test
    fun `alternarFavorito dos veces restituye el valor original`() = runTest {
        val repositorio = SenderoRepositorioMock()
        repositorio.observarSendero("usuario-1", "huella-andina").first()

        repositorio.alternarFavorito("usuario-1", "huella-andina")
        repositorio.alternarFavorito("usuario-1", "huella-andina")

        val sendero = repositorio.observarSendero("usuario-1", "huella-andina").first()
        assertFalse(sendero?.esFavorito ?: true)
    }

    // Probar que eliminarSendero lo quita: observarSendero emite null y la lista queda con uno
    @Test
    fun `eliminarSendero lo quita del detalle y del listado`() = runTest {
        val repositorio = SenderoRepositorioMock()

        repositorio.observarSendero("usuario-1", "huella-andina").test {
            assertNotNull(awaitItem())

            val resultado = repositorio.eliminarSendero("usuario-1", "huella-andina")

            assertTrue(resultado.isSuccess)
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val listaRestante = repositorio.observarSenderosDelUsuario("usuario-1").first()
        assertEquals(1, listaRestante.size)
        assertEquals("huella-neuquina", listaRestante.first().id)
    }

    // Probar que alternarFavorito con un id inexistente falla con el mensaje esperado
    @Test
    fun `alternarFavorito con id inexistente falla con mensaje`() = runTest {
        val repositorio = SenderoRepositorioMock()
        repositorio.observarSenderosDelUsuario("usuario-1").first()

        val resultado = repositorio.alternarFavorito("usuario-1", "no-existe")

        assertTrue(resultado.isFailure)
        assertEquals("No se encontró el sendero", resultado.exceptionOrNull()?.message)
    }

    // Probar que eliminarSendero con un id inexistente falla con el mensaje esperado
    @Test
    fun `eliminarSendero con id inexistente falla con mensaje`() = runTest {
        val repositorio = SenderoRepositorioMock()
        repositorio.observarSenderosDelUsuario("usuario-1").first()

        val resultado = repositorio.eliminarSendero("usuario-1", "no-existe")

        assertTrue(resultado.isFailure)
        assertEquals("No se encontró el sendero", resultado.exceptionOrNull()?.message)
    }

    // Probar que los datos de un usuario no se mezclan con los de otro
    @Test
    fun `los datos de un usuario no se mezclan con los de otro`() = runTest {
        val repositorio = SenderoRepositorioMock()
        repositorio.observarSenderosDelUsuario("usuario-1").first()
        repositorio.observarSenderosDelUsuario("usuario-2").first()

        repositorio.alternarFavorito("usuario-1", "huella-andina")
        repositorio.eliminarSendero("usuario-1", "huella-neuquina")

        val listaUsuarioDos = repositorio.observarSenderosDelUsuario("usuario-2").first()
        assertEquals(2, listaUsuarioDos.size)
        assertTrue(listaUsuarioDos.none { sendero -> sendero.esFavorito })
        assertTrue(listaUsuarioDos.all { sendero -> sendero.idUsuario == "usuario-2" })
        assertEquals(1, repositorio.observarSenderosDelUsuario("usuario-1").first().size)
    }
}
