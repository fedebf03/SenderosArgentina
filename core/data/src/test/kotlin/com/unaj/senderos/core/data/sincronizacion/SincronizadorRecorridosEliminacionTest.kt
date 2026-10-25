package com.unaj.senderos.core.data.sincronizacion

import com.unaj.senderos.core.data.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.core.data.falsos.FalsoRecorridoDao
import com.unaj.senderos.core.data.falsos.FalsoRecorridoFuenteRemota
import com.unaj.senderos.core.data.falsos.crearEntidadRecorridoDePrueba
import com.unaj.senderos.core.data.falsos.crearRecorridoRedDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Sincronizar el borrado de recorridos: se borra el documento de Firestore y recién después la fila local, sin resucitar lo borrado
class SincronizadorRecorridosEliminacionTest {

    private val idUsuario = "usuario-1"
    private val fuenteRemota = FalsoRecorridoFuenteRemota()

    private fun crearSincronizador(dao: FalsoRecorridoDao) = SincronizadorRecorridos(dao, fuenteRemota, FalsoEjecutorTransacciones())

    @Test
    fun `un recorrido eliminado se borra de la nube y despues de la base local`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 9_000L, estaEliminado = true)))
        fuenteRemota.precargar(idUsuario, crearRecorridoRedDePrueba(fechaModificacion = 1_000L))

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isSuccess)
        assertEquals(listOf(idUsuario to "recorrido-1"), fuenteRemota.recorridosEliminados)
        assertTrue(fuenteRemota.documentosPorUsuario.getValue(idUsuario).isEmpty())
        assertTrue(dao.entidades.value.isEmpty())
    }

    @Test
    fun `un recorrido eliminado nunca se vuelve a subir`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 9_000L, estaEliminado = true)))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(fuenteRemota.recorridosGuardados.isEmpty())
    }

    @Test
    fun `un recorrido eliminado que nunca llego a la nube tambien se limpia`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 9_000L, estaEliminado = true)))

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isSuccess)
        assertTrue(dao.entidades.value.isEmpty())
    }

    @Test
    fun `el documento remoto viejo no resucita un recorrido eliminado localmente`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 9_000L, estaEliminado = true)))
        fuenteRemota.precargar(idUsuario, crearRecorridoRedDePrueba(fechaModificacion = 5_000L))
        fuenteRemota.idsConErrorAlEliminar.add("recorrido-1")

        crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(dao.entidades.value.single().estaEliminado)
    }

    @Test
    fun `si falla el borrado remoto la marca de eliminado se conserva con error para reintentar`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 9_000L, estaEliminado = true)))
        fuenteRemota.precargar(idUsuario, crearRecorridoRedDePrueba(fechaModificacion = 1_000L))
        fuenteRemota.idsConErrorAlEliminar.add("recorrido-1")

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        val entidad = dao.entidades.value.single()
        assertTrue(entidad.estaEliminado)
        assertEquals("CON_ERROR", entidad.estadoSincronizacion)
    }

    @Test
    fun `un recorrido que falla al borrarse no impide borrar los demas`() = runTest {
        val dao = FalsoRecorridoDao(
            listOf(
                crearEntidadRecorridoDePrueba(id = "malo", fechaModificacion = 9_000L, estaEliminado = true),
                crearEntidadRecorridoDePrueba(id = "bueno", fechaModificacion = 9_000L, estaEliminado = true)
            )
        )
        fuenteRemota.idsConErrorAlEliminar.add("malo")

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals(listOf("malo"), dao.entidades.value.map { entidad -> entidad.id })
    }

    @Test
    fun `un recorrido modificado durante el borrado remoto no se quita de la base`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 9_000L, estaEliminado = true)))
        fuenteRemota.alEliminarRecorrido = {
            dao.guardar(crearEntidadRecorridoDePrueba(fechaModificacion = 12_000L, estaEliminado = true))
        }

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals(1, dao.entidades.value.size)
        assertEquals(12_000L, dao.entidades.value.single().fechaModificacion)
    }

    @Test
    fun `un recorrido normal pendiente sigue subiendose igual`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 2_000L)))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals(1, fuenteRemota.recorridosGuardados.size)
        assertEquals("SINCRONIZADO", dao.entidades.value.single().estadoSincronizacion)
    }
}
