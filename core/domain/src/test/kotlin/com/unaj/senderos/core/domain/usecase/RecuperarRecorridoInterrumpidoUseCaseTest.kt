package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAlmacenBorradorRecorrido
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.crearPuntoCapturadoDePrueba
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Recuperar el recorrido que quedó a medias porque Android cerró la app: se ofrece al volver a abrirla
@OptIn(ExperimentalCoroutinesApi::class)
class RecuperarRecorridoInterrumpidoUseCaseTest {

    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val almacen = FalsoAlmacenBorradorRecorrido()
    private val recorridos = FalsoRecorridoRepositorio()
    private val puntos = FalsoPuntosCapturadosRepositorio()
    private val casoDeUso = RecuperarRecorridoInterrumpidoUseCase(almacen, recorridos, puntos, autenticacion)

    private fun guardarBorrador(idUsuario: String = "usuario-1", idSendero: String = "libre-1000") {
        almacen.borrador = crearRecorridoDePrueba(id = "recorrido-1000", idUsuario = idUsuario, idSendero = idSendero)
    }

    @Test
    fun `sin borrador no hay nada que recuperar`() = runTest {
        assertNull(casoDeUso.buscar())
    }

    @Test
    fun `el borrador del usuario con sesion se ofrece`() = runTest {
        guardarBorrador()

        assertEquals("recorrido-1000", casoDeUso.buscar()?.id)
    }

    @Test
    fun `el borrador de otro usuario no se ofrece ni se toca`() = runTest {
        guardarBorrador(idUsuario = "usuario-2")

        assertNull(casoDeUso.buscar())
        assertNotNull(almacen.borrador)
    }

    @Test
    fun `sin sesion no se ofrece nada`() = runTest {
        guardarBorrador()
        autenticacion.usuarioActual = null

        assertNull(casoDeUso.buscar())
    }

    @Test
    fun `recuperar guarda el recorrido en el historial y borra el borrador`() = runTest {
        guardarBorrador()

        val resultado = casoDeUso.recuperar()

        assertTrue(resultado.isSuccess)
        assertEquals("recorrido-1000", recorridos.guardados.single().id)
        assertNull(almacen.borrador)
    }

    @Test
    fun `si no se puede guardar en el historial el borrador se conserva`() = runTest {
        guardarBorrador()
        recorridos.resultadoGuardar = Result.failure(IllegalStateException("Sin espacio"))

        val resultado = casoDeUso.recuperar()

        assertTrue(resultado.isFailure)
        assertNotNull(almacen.borrador)
    }

    @Test
    fun `recuperar sin borrador falla con un mensaje claro`() = runTest {
        val resultado = casoDeUso.recuperar()

        assertTrue(resultado.isFailure)
        assertEquals("No hay un recorrido para recuperar", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `descartar una caminata libre borra el borrador y sus puntos marcados`() = runTest {
        guardarBorrador(idSendero = "libre-1000")
        puntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "p1", idSendero = "libre-1000"),
            crearPuntoCapturadoDePrueba(id = "p2", idSendero = "otro-sendero")
        )

        val resultado = casoDeUso.descartar()

        assertTrue(resultado.isSuccess)
        assertNull(almacen.borrador)
        assertEquals(listOf("p2"), puntos.puntos.value.map { punto -> punto.id })
    }

    @Test
    fun `descartar un recorrido de sendero conserva sus puntos para revisarlos`() = runTest {
        guardarBorrador(idSendero = "sendero-1")
        puntos.puntos.value = listOf(crearPuntoCapturadoDePrueba(id = "p1", idSendero = "sendero-1"))

        casoDeUso.descartar()

        assertNull(almacen.borrador)
        assertEquals(1, puntos.puntos.value.size)
    }
}
