package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.port.ArchivoLeido
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoEscritorArchivos
import com.unaj.senderos.core.testing.FalsoFormatoCopiaSeguridad
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Exportar un sendero, exportar todos como copia de seguridad y restaurarla
class ExportacionSenderosTest {

    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val trazado = listOf(PuntoRecorrido(-41.10, -71.30), PuntoRecorrido(-41.12, -71.33))
    private val repositorio = FalsoSenderoRepositorio(
        listOf(
            crearSenderoDePrueba(id = "s1", nombre = "Cerro Otto", recorrido = trazado, tipoRecorrido = TipoRecorrido.LINEAL),
            crearSenderoDePrueba(id = "s2", nombre = "Laguna Negra", recorrido = trazado, tipoRecorrido = TipoRecorrido.LINEAL)
        )
    )
    private val escritor = FalsoEscritorArchivos()
    private val lector = FalsoLectorArchivos()
    private val formato = FalsoFormatoCopiaSeguridad()
    private val guardar = GuardarSenderoUseCase(repositorio, autenticacion, generarIdentificador = { "nuevo-id" })

    @Test
    fun `exportar un sendero en gpx escribe el gpx en el archivo elegido`() = runTest {
        val exportar = ExportarSenderoUseCase(repositorio, autenticacion, escritor, formato)

        exportar("s1", FormatoExportacion.GPX, "content://destino").getOrThrow()

        assertTrue(escritor.escritos.getValue("content://destino").contains("<name>Cerro Otto</name>"))
    }

    @Test
    fun `exportar un sendero en json escribe una copia de un solo sendero`() = runTest {
        val exportar = ExportarSenderoUseCase(repositorio, autenticacion, escritor, formato)

        exportar("s1", FormatoExportacion.JSON, "content://destino").getOrThrow()

        val copia = formato.leer(escritor.escritos.getValue("content://destino")).getOrThrow()
        assertEquals(listOf("s1"), copia.map { sendero -> sendero.id })
    }

    @Test
    fun `exportar un sendero que no existe falla`() = runTest {
        val exportar = ExportarSenderoUseCase(repositorio, autenticacion, escritor, formato)

        assertEquals("No se encontró el sendero", exportar("nada", FormatoExportacion.CSV, "content://x").exceptionOrNull()?.message)
    }

    @Test
    fun `la copia de seguridad lleva todos los senderos del usuario`() = runTest {
        val exportar = ExportarCopiaSeguridadUseCase(repositorio, autenticacion, escritor, formato)

        val cantidad = exportar("content://copia").getOrThrow()

        assertEquals(2, cantidad)
        val copia = formato.leer(escritor.escritos.getValue("content://copia")).getOrThrow()
        assertEquals(setOf("s1", "s2"), copia.map { sendero -> sendero.id }.toSet())
    }

    @Test
    fun `analizar una copia separa los senderos nuevos de los que ya existen`() = runTest {
        formato.registrar("texto-copia", listOf(crearSenderoDePrueba(id = "s1"), crearSenderoDePrueba(id = "s9", nombre = "Nuevo")))
        lector.archivos["content://copia"] = ArchivoLeido("senderos.json", "texto-copia")
        val importar = ImportarCopiaSeguridadUseCase(repositorio, autenticacion, lector, formato, guardar)

        val analisis = importar.analizar("content://copia").getOrThrow()

        assertEquals(listOf("s9"), analisis.nuevos.map { sendero -> sendero.id })
        assertEquals(listOf("s1"), analisis.existentes.map { sendero -> sendero.id })
    }

    @Test
    fun `restaurar reemplazando guarda los existentes con su mismo id`() = runTest {
        val importar = ImportarCopiaSeguridadUseCase(repositorio, autenticacion, lector, formato, guardar)
        val analisis = AnalisisCopiaSeguridad(
            nuevos = listOf(crearSenderoDePrueba(id = "s9", nombre = "Nuevo")),
            existentes = listOf(crearSenderoDePrueba(id = "s1", nombre = "Cerro Otto restaurado"))
        )

        val guardados = importar.aplicar(analisis, ModoRestauracion.REEMPLAZAR_EXISTENTES)

        assertEquals(2, guardados)
        assertEquals("Cerro Otto restaurado", repositorio.senderos.value.single { sendero -> sendero.id == "s1" }.nombre)
        assertTrue(repositorio.senderos.value.any { sendero -> sendero.id == "s9" })
    }

    @Test
    fun `reemplazar conserva el origen del sendero local`() = runTest {
        repositorio.senderos.value = listOf(crearSenderoDePrueba(id = "s1", nombre = "Cerro Otto").copy(idOrigen = "publico-7"))
        val importar = ImportarCopiaSeguridadUseCase(repositorio, autenticacion, lector, formato, guardar)

        importar.aplicar(
            AnalisisCopiaSeguridad(nuevos = emptyList(), existentes = listOf(crearSenderoDePrueba(id = "s1", nombre = "Restaurado"))),
            ModoRestauracion.REEMPLAZAR_EXISTENTES
        )

        val guardado = repositorio.senderos.value.single { sendero -> sendero.id == "s1" }
        assertEquals("Restaurado", guardado.nombre)
        assertEquals("publico-7", guardado.idOrigen)
    }

    @Test
    fun `analizar avisa los existentes con cambios sin subir y descarta ids repetidos de la copia`() = runTest {
        repositorio.senderos.value = listOf(
            crearSenderoDePrueba(id = "s1", estadoSincronizacion = com.unaj.senderos.core.domain.model.EstadoSincronizacion.PENDIENTE)
        )
        formato.registrar("texto", listOf(crearSenderoDePrueba(id = "s1"), crearSenderoDePrueba(id = "s9"), crearSenderoDePrueba(id = "s9")))
        lector.archivos["content://copia"] = ArchivoLeido("senderos.json", "texto")
        val importar = ImportarCopiaSeguridadUseCase(repositorio, autenticacion, lector, formato, guardar)

        val analisis = importar.analizar("content://copia").getOrThrow()

        assertEquals(1, analisis.existentesConCambiosSinSubir)
        assertEquals(listOf("s9"), analisis.nuevos.map { sendero -> sendero.id })
    }

    @Test
    fun `restaurar como copias guarda los existentes con otro id y nombre marcado`() = runTest {
        val importar = ImportarCopiaSeguridadUseCase(repositorio, autenticacion, lector, formato, guardar)
        val analisis = AnalisisCopiaSeguridad(nuevos = emptyList(), existentes = listOf(crearSenderoDePrueba(id = "s1", nombre = "Cerro Otto")))

        importar.aplicar(analisis, ModoRestauracion.AGREGAR_COMO_COPIAS)

        val copia = repositorio.senderos.value.single { sendero -> sendero.id == "nuevo-id" }
        assertEquals("Cerro Otto (copia)", copia.nombre)
        assertEquals("Cerro Otto", repositorio.senderos.value.single { sendero -> sendero.id == "s1" }.nombre)
    }

    @Test
    fun `un archivo que no es una copia falla con un mensaje claro`() = runTest {
        lector.archivos["content://otro"] = ArchivoLeido("foto.jpg", "cualquier cosa")
        val importar = ImportarCopiaSeguridadUseCase(repositorio, autenticacion, lector, formato, guardar)

        assertEquals(
            "El archivo no es una copia de seguridad de Senderos",
            importar.analizar("content://otro").exceptionOrNull()?.message
        )
    }
}
