package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.crearPuntoCapturadoDePrueba
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Borrar un recorrido del historial; si era una caminata libre sin convertir, también los puntos que había marcado
@OptIn(ExperimentalCoroutinesApi::class)
class EliminarRecorridoUseCaseTest {

    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val recorridos = FalsoRecorridoRepositorio()
    private val puntos = FalsoPuntosCapturadosRepositorio()
    private val casoDeUso = EliminarRecorridoUseCase(recorridos, puntos, autenticacion)

    private fun guardarRecorrido(id: String = "r1", idSendero: String) {
        recorridos.recorridos.value = listOf(crearRecorridoDePrueba(id = id, idSendero = idSendero))
    }

    @Test
    fun `elimina el recorrido del usuario con sesion`() = runTest {
        guardarRecorrido(idSendero = "sendero-1")

        val resultado = casoDeUso("r1")

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("usuario-1" to "r1"), recorridos.eliminados)
    }

    @Test
    fun `una caminata libre sin convertir descarta tambien sus puntos marcados`() = runTest {
        guardarRecorrido(idSendero = "libre-1000")
        puntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "p1", idSendero = "libre-1000"),
            crearPuntoCapturadoDePrueba(id = "p2", idSendero = "libre-9999"),
            crearPuntoCapturadoDePrueba(id = "p3", idSendero = "sendero-1")
        )

        casoDeUso("r1")

        assertEquals(listOf("p2", "p3"), puntos.puntos.value.map { punto -> punto.id })
        assertEquals(setOf("p1"), puntos.llamadasDescartar.single().second.toSet())
    }

    @Test
    fun `un recorrido de sendero conserva los puntos que se marcaron en el`() = runTest {
        guardarRecorrido(idSendero = "sendero-1")
        puntos.puntos.value = listOf(crearPuntoCapturadoDePrueba(id = "p1", idSendero = "sendero-1"))

        casoDeUso("r1")

        assertEquals(1, puntos.puntos.value.size)
        assertTrue(puntos.llamadasDescartar.isEmpty())
    }

    @Test
    fun `un recorrido que no existe no hace nada y no falla`() = runTest {
        val resultado = casoDeUso("inexistente")

        assertTrue(resultado.isSuccess)
        assertTrue(recorridos.eliminados.isEmpty())
    }

    @Test
    fun `sin sesion falla y no toca nada`() = runTest {
        guardarRecorrido(idSendero = "libre-1000")
        autenticacion.usuarioActual = null

        val resultado = casoDeUso("r1")

        assertTrue(resultado.isFailure)
        assertTrue(recorridos.eliminados.isEmpty())
    }

    @Test
    fun `si no se puede eliminar el recorrido sus puntos quedan intactos`() = runTest {
        guardarRecorrido(idSendero = "libre-1000")
        puntos.puntos.value = listOf(crearPuntoCapturadoDePrueba(id = "p1", idSendero = "libre-1000"))
        recorridos.resultadoEliminar = Result.failure(IllegalStateException("Disco lleno"))

        val resultado = casoDeUso("r1")

        assertTrue(resultado.isFailure)
        assertEquals(1, puntos.puntos.value.size)
    }

    @Test
    fun `si falla el descarte de los puntos el recorrido igual queda eliminado`() = runTest {
        guardarRecorrido(idSendero = "libre-1000")
        puntos.puntos.value = listOf(crearPuntoCapturadoDePrueba(id = "p1", idSendero = "libre-1000"))
        puntos.resultadoDescartar = Result.failure(IllegalStateException("Sin acceso a los archivos"))

        val resultado = casoDeUso("r1")

        assertTrue(resultado.isSuccess)
        assertEquals(1, recorridos.eliminados.size)
    }
}
