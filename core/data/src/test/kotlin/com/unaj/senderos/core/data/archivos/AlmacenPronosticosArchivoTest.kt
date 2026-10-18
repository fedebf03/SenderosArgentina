package com.unaj.senderos.core.data.archivos

import com.unaj.senderos.core.domain.clima.ClimaActual
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.clima.PronosticoClima
import com.unaj.senderos.core.domain.clima.PronosticoDia
import com.unaj.senderos.core.domain.model.Coordenada
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Último pronóstico de cada lugar guardado en archivos de la app, para verlo sin conexión
class AlmacenPronosticosArchivoTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private fun crearAlmacen() = AlmacenPronosticosArchivo(File(carpeta.root, "pronosticos"))

    private fun pronostico(obtenido: Long = 5_000L, sensacion: Double? = 8.5) = PronosticoClima(
        lugar = Coordenada(-41.13, -71.30),
        obtenidoMilisegundos = obtenido,
        actual = ClimaActual(11.4, sensacion, 22.0, 41.5, CondicionClima.NUBLADO, esDeDia = false),
        dias = listOf(
            PronosticoDia("2026-10-19", 16.2, 4.1, 60, 3.4, 28.0, 45.0, CondicionClima.LLUVIA),
            PronosticoDia("2026-10-20", 14.0, 2.5, null, 22.0, 44.0, 70.0, CondicionClima.TORMENTA_CON_GRANIZO)
        )
    )

    @Test
    fun `sin nada guardado no hay pronostico`() = runTest {
        assertNull(crearAlmacen().leer("clima_-4113_-7130"))
    }

    @Test
    fun `lo que se guarda se lee igual`() = runTest {
        val almacen = crearAlmacen()
        val original = pronostico()

        assertTrue(almacen.guardar("clima_-4113_-7130", original).isSuccess)

        assertEquals(original, almacen.leer("clima_-4113_-7130"))
    }

    @Test
    fun `los datos desconocidos se conservan como desconocidos`() = runTest {
        val almacen = crearAlmacen()
        val original = pronostico(sensacion = null)

        almacen.guardar("clima_-4113_-7130", original)

        assertEquals(original, almacen.leer("clima_-4113_-7130"))
    }

    @Test
    fun `cada lugar tiene su propia copia`() = runTest {
        val almacen = crearAlmacen()
        almacen.guardar("clima_-4113_-7130", pronostico(obtenido = 1L))
        almacen.guardar("clima_-3478_-5821", pronostico(obtenido = 2L))

        assertEquals(1L, almacen.leer("clima_-4113_-7130")?.obtenidoMilisegundos)
        assertEquals(2L, almacen.leer("clima_-3478_-5821")?.obtenidoMilisegundos)
    }

    @Test
    fun `guardar de nuevo reemplaza la copia anterior del lugar`() = runTest {
        val almacen = crearAlmacen()
        almacen.guardar("clima_-4113_-7130", pronostico(obtenido = 1L))

        almacen.guardar("clima_-4113_-7130", pronostico(obtenido = 9L))

        assertEquals(9L, almacen.leer("clima_-4113_-7130")?.obtenidoMilisegundos)
        assertEquals(1, File(carpeta.root, "pronosticos").list()?.size)
    }

    @Test
    fun `un archivo danado se trata como si no hubiera copia`() = runTest {
        val almacen = crearAlmacen()
        almacen.guardar("clima_-4113_-7130", pronostico())
        File(carpeta.root, "pronosticos/clima_-4113_-7130.json").writeText("{ no es json")

        assertNull(almacen.leer("clima_-4113_-7130"))
    }

    @Test
    fun `una condicion desconocida en el archivo se lee como variable`() = runTest {
        val almacen = crearAlmacen()
        almacen.guardar("clima_-4113_-7130", pronostico())
        val archivo = File(carpeta.root, "pronosticos/clima_-4113_-7130.json")
        archivo.writeText(archivo.readText().replace("NUBLADO", "CONDICION_DEL_FUTURO"))

        assertEquals(CondicionClima.VARIABLE, almacen.leer("clima_-4113_-7130")?.actual?.condicion)
    }

    @Test
    fun `una clave con caracteres raros no escapa de la carpeta`() = runTest {
        val almacen = crearAlmacen()

        assertTrue(almacen.guardar("../../afuera", pronostico()).isFailure)
        assertTrue(carpeta.root.list()?.none { nombre -> nombre.contains("afuera") } ?: true)
    }
}
