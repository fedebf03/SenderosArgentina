package com.unaj.senderos.core.data.repository

import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.data.falsos.FalsoPlanificadorSincronizacion
import com.unaj.senderos.core.data.falsos.FalsoRecorridoDao
import com.unaj.senderos.core.data.falsos.crearEntidadRecorridoDePrueba
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorridoRepositorioOfflineFirstTest {

    private val dao = FalsoRecorridoDao()
    private val planificador = FalsoPlanificadorSincronizacion()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private val repositorio = RecorridoRepositorioOfflineFirst(dao, planificador, autenticacion) { 8_000L }

    @Test
    fun `guardar escribe pendiente en Room y pide sincronizacion`() = runTest {
        val resultado = repositorio.guardar(crearRecorridoDePrueba(estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO))

        assertTrue(resultado.isSuccess)
        assertEquals("PENDIENTE", dao.entidades.value.single().estadoSincronizacion)
        assertEquals(8_000L, dao.entidades.value.single().fechaModificacion)
        assertEquals(listOf("usuario-1"), planificador.solicitudes)
    }

    @Test
    fun `un fallo local devuelve failure y no pide sincronizacion`() = runTest {
        dao.errorAlGuardar = IllegalStateException("Disco lleno")

        val resultado = repositorio.guardar(crearRecorridoDePrueba())

        assertTrue(resultado.isFailure)
        assertTrue(planificador.solicitudes.isEmpty())
    }

    @Test
    fun `observar emite del mas reciente al mas viejo solo del usuario`() = runTest {
        repositorio.guardar(crearRecorridoDePrueba(id = "viejo", inicioMilisegundos = 1_000L, finMilisegundos = 2_000L))
        repositorio.guardar(crearRecorridoDePrueba(id = "nuevo", inicioMilisegundos = 5_000L, finMilisegundos = 6_000L))
        repositorio.guardar(crearRecorridoDePrueba(id = "ajeno", idUsuario = "usuario-2"))

        val lista = repositorio.observarRecorridos("usuario-1").first()

        assertEquals(listOf("nuevo", "viejo"), lista.map { recorrido -> recorrido.id })
    }

    @Test
    fun `observar la lista solicita la sincronizacion`() = runTest {
        repositorio.observarRecorridos("usuario-1").first()

        assertEquals(listOf("usuario-1"), planificador.solicitudes)
    }

    @Test
    fun `observar un recorrido devuelve el dominio o null`() = runTest {
        dao.guardar(crearEntidadRecorridoDePrueba())

        assertEquals("recorrido-1", repositorio.observarRecorrido("usuario-1", "recorrido-1").first()?.id)
        assertNull(repositorio.observarRecorrido("usuario-1", "otro").first())
    }

    @Test
    fun `guardar sin sesion del duenio no pide sincronizacion`() = runTest {
        autenticacion.usuarioActual = null

        val resultado = repositorio.guardar(crearRecorridoDePrueba())

        assertTrue(resultado.isSuccess)
        assertEquals("PENDIENTE", dao.entidades.value.single().estadoSincronizacion)
        assertTrue(planificador.solicitudes.isEmpty())
    }

    @Test
    fun `guardar con sesion de otro usuario no pide sincronizacion`() = runTest {
        autenticacion.usuarioActual = Usuario(id = "usuario-2", email = "b@correo.com", nombre = "Beto")

        repositorio.guardar(crearRecorridoDePrueba())

        assertEquals(1, dao.entidades.value.size)
        assertTrue(planificador.solicitudes.isEmpty())
    }
}
