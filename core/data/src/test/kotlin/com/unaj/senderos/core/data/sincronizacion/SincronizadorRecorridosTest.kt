package com.unaj.senderos.core.data.sincronizacion

import com.unaj.senderos.core.data.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.core.data.falsos.FalsoRecorridoDao
import com.unaj.senderos.core.data.falsos.FalsoRecorridoFuenteRemota
import com.unaj.senderos.core.data.falsos.crearEntidadRecorridoDePrueba
import com.unaj.senderos.core.data.falsos.crearRecorridoRedDePrueba
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SincronizadorRecorridosTest {

    private val idUsuario = "usuario-1"
    private val fuenteRemota = FalsoRecorridoFuenteRemota()

    private fun crearSincronizador(dao: FalsoRecorridoDao): SincronizadorRecorridos {
        return SincronizadorRecorridos(dao, fuenteRemota, FalsoEjecutorTransacciones())
    }

    @Test
    fun `un recorrido pendiente se sube y queda sincronizado`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 2_000L)))

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isSuccess)
        assertEquals(1, fuenteRemota.recorridosGuardados.size)
        assertEquals(2_000L, fuenteRemota.documentosPorUsuario.getValue(idUsuario).getValue("recorrido-1").fechaModificacion)
        assertEquals("SINCRONIZADO", dao.entidades.value.single().estadoSincronizacion)
    }

    @Test
    fun `un remoto mas nuevo reemplaza al local`() = runTest {
        val dao = FalsoRecorridoDao(
            listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 1_000L, estadoSincronizacion = "SINCRONIZADO"))
        )
        fuenteRemota.precargar(idUsuario, crearRecorridoRedDePrueba(fechaModificacion = 5_000L).copy(nombreSendero = "Remoto"))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals("Remoto", dao.entidades.value.single().nombreSendero)
        assertEquals(5_000L, dao.entidades.value.single().fechaModificacion)
        assertEquals("SINCRONIZADO", dao.entidades.value.single().estadoSincronizacion)
    }

    @Test
    fun `un remoto mas viejo no pisa al local pendiente`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 9_000L)))
        fuenteRemota.precargar(idUsuario, crearRecorridoRedDePrueba(fechaModificacion = 5_000L).copy(nombreSendero = "Remoto"))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals("Cerro Catedral", dao.entidades.value.single().nombreSendero)
        assertEquals(9_000L, fuenteRemota.documentosPorUsuario.getValue(idUsuario).getValue("recorrido-1").fechaModificacion)
    }

    @Test
    fun `un remoto desconocido se incorpora como sincronizado`() = runTest {
        val dao = FalsoRecorridoDao()
        fuenteRemota.precargar(idUsuario, crearRecorridoRedDePrueba(id = "nuevo", fechaModificacion = 3_000L))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals("nuevo", dao.entidades.value.single().id)
        assertEquals("SINCRONIZADO", dao.entidades.value.single().estadoSincronizacion)
        assertTrue(fuenteRemota.recorridosGuardados.isEmpty())
    }

    @Test
    fun `si falla la lectura remota se devuelve el error`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba()))
        fuenteRemota.errorAlObtener = IllegalStateException("Sin red")

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertTrue(fuenteRemota.recorridosGuardados.isEmpty())
        assertEquals("PENDIENTE", dao.entidades.value.single().estadoSincronizacion)
    }

    @Test
    fun `un recorrido que falla queda con error y los demas se suben`() = runTest {
        val dao = FalsoRecorridoDao(
            listOf(
                crearEntidadRecorridoDePrueba(id = "malo"),
                crearEntidadRecorridoDePrueba(id = "bueno")
            )
        )
        fuenteRemota.idsConErrorAlGuardar.add("malo")

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertEquals("CON_ERROR", dao.entidades.value.first { entidad -> entidad.id == "malo" }.estadoSincronizacion)
        assertEquals("SINCRONIZADO", dao.entidades.value.first { entidad -> entidad.id == "bueno" }.estadoSincronizacion)
    }

    @Test
    fun `un recorrido modificado durante la subida sigue pendiente`() = runTest {
        val dao = FalsoRecorridoDao(listOf(crearEntidadRecorridoDePrueba(fechaModificacion = 2_000L)))
        fuenteRemota.alGuardarRecorrido = {
            dao.guardar(crearEntidadRecorridoDePrueba(fechaModificacion = 3_000L))
        }

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals("PENDIENTE", dao.entidades.value.single().estadoSincronizacion)
        assertEquals(3_000L, dao.entidades.value.single().fechaModificacion)
    }

    @Test
    fun `la cancelacion se relanza`() = runTest {
        val dao = FalsoRecorridoDao()
        fuenteRemota.errorAlObtener = CancellationException("Cancelado")

        try {
            crearSincronizador(dao).sincronizar(idUsuario)
            fail("Se esperaba CancellationException")
        } catch (excepcion: CancellationException) {
            assertEquals("Cancelado", excepcion.message)
        }
    }
}
