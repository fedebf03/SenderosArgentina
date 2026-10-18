package com.unaj.senderos.core.data.repository

import com.unaj.senderos.core.data.falsos.FalsoPlanificadorSincronizacion
import com.unaj.senderos.core.data.falsos.FalsoRecorridoDao
import com.unaj.senderos.core.data.falsos.crearEntidadRecorridoDePrueba
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Borrar un recorrido del historial: se marca como eliminado, desaparece de las listas y se sincroniza para borrarlo también de Firestore
class RecorridoRepositorioEliminarTest {

    private val dao = FalsoRecorridoDao(
        listOf(
            crearEntidadRecorridoDePrueba(id = "a", fechaModificacion = 1_000L, estadoSincronizacion = "SINCRONIZADO"),
            crearEntidadRecorridoDePrueba(id = "b", fechaModificacion = 1_000L, estadoSincronizacion = "SINCRONIZADO")
        )
    )
    private val planificador = FalsoPlanificadorSincronizacion()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private val repositorio = RecorridoRepositorioOfflineFirst(dao, planificador, autenticacion) { 9_000L }

    @Test
    fun `eliminar marca el recorrido como eliminado y pendiente con la fecha actual`() = runTest {
        val resultado = repositorio.eliminar("usuario-1", "a")

        assertTrue(resultado.isSuccess)
        val marcado = dao.entidades.value.first { entidad -> entidad.id == "a" }
        assertTrue(marcado.estaEliminado)
        assertEquals("PENDIENTE", marcado.estadoSincronizacion)
        assertEquals(9_000L, marcado.fechaModificacion)
    }

    @Test
    fun `un recorrido eliminado ya no aparece en la lista ni en su detalle`() = runTest {
        repositorio.eliminar("usuario-1", "a")

        assertEquals(listOf("b"), repositorio.observarRecorridos("usuario-1").first().map { recorrido -> recorrido.id })
        assertNull(repositorio.observarRecorrido("usuario-1", "a").first())
    }

    @Test
    fun `eliminar pide la sincronizacion para borrarlo tambien de la nube`() = runTest {
        repositorio.eliminar("usuario-1", "a")

        assertEquals(listOf("usuario-1"), planificador.solicitudes)
    }

    @Test
    fun `eliminar un recorrido que no existe no falla ni sincroniza`() = runTest {
        val resultado = repositorio.eliminar("usuario-1", "inexistente")

        assertTrue(resultado.isSuccess)
        assertTrue(planificador.solicitudes.isEmpty())
    }

    @Test
    fun `eliminar solo toca el recorrido del usuario indicado`() = runTest {
        val ajeno = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(id = "a", idUsuario = "usuario-2")))
        val otroRepositorio = RecorridoRepositorioOfflineFirst(ajeno, planificador, autenticacion) { 9_000L }

        otroRepositorio.eliminar("usuario-1", "a")

        assertTrue(!ajeno.entidades.value.single().estaEliminado)
    }

    @Test
    fun `un fallo local al eliminar devuelve failure y no sincroniza`() = runTest {
        dao.errorAlGuardar = IllegalStateException("Disco lleno")

        val resultado = repositorio.eliminar("usuario-1", "a")

        assertTrue(resultado.isFailure)
        assertTrue(planificador.solicitudes.isEmpty())
    }

    @Test
    fun `sin la sesion del dueno se elimina localmente y la nube se limpia en su proximo ingreso`() = runTest {
        autenticacion.usuarioActual = null

        repositorio.eliminar("usuario-1", "a")

        assertTrue(dao.entidades.value.first { entidad -> entidad.id == "a" }.estaEliminado)
        assertTrue(planificador.solicitudes.isEmpty())
    }
}
