package com.unaj.senderos.core.data.archivos

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Borrador del recorrido en curso guardado en un archivo de la app
class AlmacenBorradorRecorridoArchivoTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private fun crearAlmacen(nombre: String = "borrador.json"): Pair<AlmacenBorradorRecorridoArchivo, File> {
        val archivo = File(carpeta.root, nombre)
        return AlmacenBorradorRecorridoArchivo(archivo) to archivo
    }

    private fun recorrido(id: String = "recorrido-1000", pasos: Int? = 1800) = RecorridoRealizado(
        id = id,
        idUsuario = "usuario-1",
        idSendero = "libre-1000",
        nombreSendero = "Caminata libre",
        inicioMilisegundos = 1_000L,
        finMilisegundos = 61_000L,
        duracionMilisegundos = 60_000L,
        distanciaKilometros = 1.25,
        velocidadPromedioKilometrosPorHora = 4.5,
        velocidadMaximaKilometrosPorHora = 6.1,
        pasos = pasos,
        ritmoPasosPorMinuto = if (pasos == null) null else 96.5,
        trazadoCaminado = listOf(Coordenada(-41.13, -71.30), Coordenada(-41.131, -71.299)),
        esSimulado = false,
        estadoSincronizacion = EstadoSincronizacion.PENDIENTE
    )

    @Test
    fun `sin borrador guardado no hay nada que leer`() = runTest {
        val (almacen, _) = crearAlmacen()

        assertNull(almacen.leer())
    }

    @Test
    fun `lo que se guarda se lee igual`() = runTest {
        val (almacen, _) = crearAlmacen()
        val original = recorrido()

        assertTrue(almacen.guardar(original).isSuccess)

        assertEquals(original, almacen.leer())
    }

    @Test
    fun `los pasos y el ritmo nulos de un equipo sin acelerometro se conservan`() = runTest {
        val (almacen, _) = crearAlmacen()
        val original = recorrido(pasos = null)

        almacen.guardar(original)

        assertEquals(original, almacen.leer())
    }

    @Test
    fun `guardar de nuevo reemplaza el borrador anterior`() = runTest {
        val (almacen, _) = crearAlmacen()
        almacen.guardar(recorrido(id = "recorrido-1000"))

        almacen.guardar(recorrido(id = "recorrido-2000"))

        assertEquals("recorrido-2000", almacen.leer()?.id)
    }

    @Test
    fun `borrar elimina el borrador`() = runTest {
        val (almacen, archivo) = crearAlmacen()
        almacen.guardar(recorrido())

        almacen.borrar()

        assertNull(almacen.leer())
        assertFalse(archivo.exists())
    }

    @Test
    fun `borrar sin borrador no falla`() = runTest {
        val (almacen, _) = crearAlmacen()

        almacen.borrar()

        assertNull(almacen.leer())
    }

    @Test
    fun `un archivo danado se trata como si no hubiera borrador`() = runTest {
        val (almacen, archivo) = crearAlmacen()
        archivo.writeText("{ esto no es json")

        assertNull(almacen.leer())
    }

    @Test
    fun `no quedan archivos temporales despues de guardar`() = runTest {
        val (almacen, _) = crearAlmacen()

        almacen.guardar(recorrido())

        assertEquals(listOf("borrador.json"), carpeta.root.list()?.toList())
    }
}
