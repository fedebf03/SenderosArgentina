package com.unaj.senderos.data.repository

import app.cash.turbine.test
import com.unaj.senderos.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.falsos.FalsoPlanificadorSincronizacion
import com.unaj.senderos.falsos.FalsoSenderoDao
import com.unaj.senderos.falsos.crearEntidadDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderoRepositorioOfflineFirstTest {

    private val instanteFijo = 50_000L
    private val falsoDao = FalsoSenderoDao()
    private val falsoPlanificador = FalsoPlanificadorSincronizacion()
    private val falsoEjecutor = FalsoEjecutorTransacciones()

    private fun crearRepositorio(): SenderoRepositorioOfflineFirst {
        return SenderoRepositorioOfflineFirst(
            senderoDao = falsoDao,
            planificadorSincronizacion = falsoPlanificador,
            ejecutorTransacciones = falsoEjecutor,
            reloj = { instanteFijo }
        )
    }

    @Test
    fun `observar emite desde el DAO y solicita sincronizacion al empezar`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", nombre = "Alfa"))
        val repositorio = crearRepositorio()

        repositorio.observarSenderosDelUsuario("usuario-1").test {
            val lista = awaitItem()
            assertEquals(listOf("a"), lista.map { sendero -> sendero.id })
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("usuario-1"), falsoPlanificador.idsUsuarioSolicitados)
    }

    @Test
    fun `observar sin red devuelve los datos locales`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Alfa"),
            crearEntidadDePrueba(id = "b", nombre = "Beta")
        )

        val lista = crearRepositorio().observarSenderosDelUsuario("usuario-1").first()

        assertEquals(listOf("Alfa", "Beta"), lista.map { sendero -> sendero.nombre })
    }

    @Test
    fun `observar excluye eliminados y senderos de otros usuarios`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Alfa"),
            crearEntidadDePrueba(id = "b", nombre = "Beta", estaEliminado = true),
            crearEntidadDePrueba(id = "c", idUsuario = "usuario-2", nombre = "Gamma")
        )

        crearRepositorio().observarSenderosDelUsuario("usuario-1").test {
            assertEquals(listOf("a"), awaitItem().map { sendero -> sendero.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `la lista observada se actualiza sola al cambiar el DAO`() = runTest {
        val repositorio = crearRepositorio()

        repositorio.observarSenderosDelUsuario("usuario-1").test {
            assertTrue(awaitItem().isEmpty())

            falsoDao.guardar(crearEntidadDePrueba(id = "nuevo", nombre = "Nuevo"))

            assertEquals(listOf("nuevo"), awaitItem().map { sendero -> sendero.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observarSendero emite el sendero o null si no existe`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a"))
        val repositorio = crearRepositorio()

        assertNotNull(repositorio.observarSendero("usuario-1", "a").first())
        assertNull(repositorio.observarSendero("usuario-1", "inexistente").first())
    }

    @Test
    fun `guardarSendero guarda PENDIENTE con la fecha del reloj y solicita sincronizacion`() = runTest {
        val repositorio = crearRepositorio()

        val resultado = repositorio.guardarSendero(
            "usuario-1",
            crearSenderoDePrueba(id = "nuevo", idUsuario = "otro")
        )

        resultado.getOrThrow()
        val guardada = falsoDao.entidades.value.single()
        assertEquals("nuevo", guardada.id)
        assertEquals("usuario-1", guardada.idUsuario)
        assertEquals("PENDIENTE", guardada.estadoSincronizacion)
        assertEquals(instanteFijo, guardada.fechaModificacion)
        assertFalse(guardada.estaEliminado)
        assertEquals(listOf("usuario-1"), falsoPlanificador.idsUsuarioSolicitados)
    }

    @Test
    fun `guardarSendero funciona sin red y el sendero queda visible al observar`() = runTest {
        val repositorio = crearRepositorio()

        repositorio.guardarSendero("usuario-1", crearSenderoDePrueba(id = "offline")).getOrThrow()

        repositorio.observarSendero("usuario-1", "offline").test {
            assertEquals("offline", awaitItem()?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `guardarSendero sobre uno existente lo reemplaza y lo marca PENDIENTE`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", nombre = "Viejo"))

        crearRepositorio().guardarSendero("usuario-1", crearSenderoDePrueba(id = "a", nombre = "Nuevo")).getOrThrow()

        val guardada = falsoDao.entidades.value.single()
        assertEquals("Nuevo", guardada.nombre)
        assertEquals("PENDIENTE", guardada.estadoSincronizacion)
    }

    @Test
    fun `alternarFavorito invierte esFavorito y marca PENDIENTE`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", esFavorito = false))
        val repositorio = crearRepositorio()

        repositorio.alternarFavorito("usuario-1", "a").getOrThrow()

        val primera = falsoDao.entidades.value.single()
        assertTrue(primera.esFavorito)
        assertEquals("PENDIENTE", primera.estadoSincronizacion)
        assertEquals(instanteFijo, primera.fechaModificacion)
        assertEquals(listOf("usuario-1"), falsoPlanificador.idsUsuarioSolicitados)

        repositorio.alternarFavorito("usuario-1", "a").getOrThrow()

        assertFalse(falsoDao.entidades.value.single().esFavorito)
    }

    @Test
    fun `alternarFavorito con id inexistente falla con mensaje`() = runTest {
        val resultado = crearRepositorio().alternarFavorito("usuario-1", "no-existe")

        assertTrue(resultado.exceptionOrNull() is NoSuchElementException)
        assertEquals("No se encontró el sendero", resultado.exceptionOrNull()?.message)
        assertTrue(falsoPlanificador.idsUsuarioSolicitados.isEmpty())
    }

    @Test
    fun `alternarFavorito sobre un eliminado falla con mensaje`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", estaEliminado = true))

        val resultado = crearRepositorio().alternarFavorito("usuario-1", "a")

        assertEquals("No se encontró el sendero", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `eliminarSendero marca eliminado PENDIENTE y desaparece de observar`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", nombre = "Alfa"))
        val repositorio = crearRepositorio()

        repositorio.observarSenderosDelUsuario("usuario-1").test {
            assertEquals(1, awaitItem().size)

            repositorio.eliminarSendero("usuario-1", "a").getOrThrow()

            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        val fila = falsoDao.entidades.value.single()
        assertTrue(fila.estaEliminado)
        assertEquals("PENDIENTE", fila.estadoSincronizacion)
        assertEquals(instanteFijo, fila.fechaModificacion)
        assertTrue(falsoPlanificador.idsUsuarioSolicitados.contains("usuario-1"))
    }

    @Test
    fun `eliminarSendero con id inexistente falla con mensaje`() = runTest {
        val resultado = crearRepositorio().eliminarSendero("usuario-1", "no-existe")

        assertTrue(resultado.exceptionOrNull() is NoSuchElementException)
        assertEquals("No se encontró el sendero", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `alternarFavorito pasa por el ejecutor de transacciones`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a"))

        crearRepositorio().alternarFavorito("usuario-1", "a").getOrThrow()

        assertEquals(1, falsoEjecutor.cantidadLlamadas)
    }

    @Test
    fun `eliminarSendero pasa por el ejecutor de transacciones`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a"))

        crearRepositorio().eliminarSendero("usuario-1", "a").getOrThrow()

        assertEquals(1, falsoEjecutor.cantidadLlamadas)
    }

    @Test
    fun `detenerSincronizacion cancela el trabajo del usuario`() {
        crearRepositorio().detenerSincronizacion("usuario-1")

        assertEquals(listOf("usuario-1"), falsoPlanificador.idsUsuarioCancelados)
    }

    @Test
    fun `la cancelacion al guardar se propaga`() = runTest {
        falsoDao.errorAlGuardar = CancellationException("cancelado")

        val resultado = runCatching {
            crearRepositorio().guardarSendero("usuario-1", crearSenderoDePrueba(id = "a"))
        }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
        assertTrue(falsoPlanificador.idsUsuarioSolicitados.isEmpty())
    }

    @Test
    fun `la cancelacion al alternar favorito se propaga`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a"))
        falsoDao.errorAlGuardar = CancellationException("cancelado")

        val resultado = runCatching { crearRepositorio().alternarFavorito("usuario-1", "a") }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `un fallo comun al guardar se devuelve como failure`() = runTest {
        falsoDao.errorAlGuardar = IllegalStateException("disco lleno")

        val resultado = crearRepositorio().guardarSendero("usuario-1", crearSenderoDePrueba(id = "a"))

        assertEquals("disco lleno", resultado.exceptionOrNull()?.message)
    }
}
