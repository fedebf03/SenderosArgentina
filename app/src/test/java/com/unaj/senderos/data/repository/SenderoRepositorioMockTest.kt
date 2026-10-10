package com.unaj.senderos.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderoRepositorioMockTest {

    // Probar que devuelve dos senderos de Huella Andina y Huella Neuquina con el idUsuario pedido
    @Test
    fun `devuelve dos senderos del usuario pedido`() = runTest {
        val repositorio = SenderoRepositorioMock()

        val resultado = repositorio.obtenerSenderosDelUsuario("usuario-7")

        val senderos = resultado.getOrNull().orEmpty()
        assertTrue(resultado.isSuccess)
        assertEquals(2, senderos.size)
        assertEquals(
            setOf("Huella Andina", "Huella Neuquina"),
            senderos.map { sendero -> sendero.nombre }.toSet()
        )
        assertTrue(senderos.all { sendero -> sendero.idUsuario == "usuario-7" })
    }
}
