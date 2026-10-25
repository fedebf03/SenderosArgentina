package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Exportar el trazado de un sendero en GPX y CSV para usarlo en otras aplicaciones o volver a importarlo
class EscritorTrazadosTest {

    private val ida = listOf(
        PuntoRecorrido(-50.3350, -72.2693),
        PuntoRecorrido(-50.3352, -72.2800, tipo = TipoPunto.MIRADOR, nombre = "Mirador del lago & glaciar"),
        PuntoRecorrido(-50.3360, -72.2900)
    )

    // Armar un sendero de ida y vuelta con el trazado de prueba y su perfil calculado con alturas que suben de a 10 m
    private fun senderoIdaYVuelta() = crearSenderoDePrueba(
        nombre = "Costanera <El Calafate>",
        recorrido = ida,
        tipoRecorrido = TipoRecorrido.IDA_Y_VUELTA
    ).let { sendero ->
        val muestras = CalculadoraPerfil.muestrearTrazado(sendero.recorrido, sendero.tipoRecorrido)
        val mitad = muestras.last().distanciaKilometros / 2
        val altitudes = muestras.map { muestra -> 180.0 + 10.0 * (mitad - kotlin.math.abs(muestra.distanciaKilometros - mitad)) }
        sendero.copy(perfilElevacion = CalculadoraPerfil.calcular(muestras, altitudes, CalculadoraPerfil.firmaTrazado(ida, TipoRecorrido.IDA_Y_VUELTA)))
    }

    @Test
    fun `el gpx trae el nombre escapado, el trazado completo con la vuelta y los puntos de interes`() {
        val gpx = EscritorTrazados.escribirGpx(senderoIdaYVuelta())

        assertTrue(gpx.startsWith("<?xml"))
        assertTrue(gpx.contains("<name>Costanera &lt;El Calafate&gt;</name>"))
        assertEquals(5, Regex("<trkpt ").findAll(gpx).count())
        assertEquals(1, Regex("<wpt ").findAll(gpx).count())
        assertTrue(gpx.contains("<name>Mirador del lago &amp; glaciar</name>"))
        assertTrue(gpx.contains("<ele>"))
    }

    @Test
    fun `un gpx exportado se vuelve a importar con el mismo tipo, nombre y puntos de interes`() {
        val sendero = senderoIdaYVuelta()

        val leido = LectorTrazados.leer(EscritorTrazados.escribirGpx(sendero), "x.gpx").getOrThrow()
        val importado = ImportadorTrazado.importar(leido, "x.gpx").getOrThrow()

        assertEquals(TipoRecorrido.IDA_Y_VUELTA, importado.tipoRecorrido)
        assertEquals("Costanera <El Calafate>", importado.nombre)
        assertEquals("Mirador del lago & glaciar", importado.recorrido.single { punto -> punto.tipo == TipoPunto.OTRO }.nombre)
        assertTrue(importado.perfil != null)
    }

    @Test
    fun `el csv usa el mismo encabezado que las aplicaciones de senderismo`() {
        val lineas = EscritorTrazados.escribirCsv(senderoIdaYVuelta()).lines().filter { linea -> linea.isNotBlank() }

        assertEquals("Latitude,Longitude,Elevation", lineas.first())
        assertEquals(6, lineas.size)
        assertTrue(lineas[1].startsWith("-50.335,-72.2693,"))
    }

    @Test
    fun `sin perfil el csv deja la altura vacia y el gpx no trae altura`() {
        val sendero = crearSenderoDePrueba(recorrido = ida, tipoRecorrido = TipoRecorrido.LINEAL)

        assertEquals("-50.335,-72.2693,", EscritorTrazados.escribirCsv(sendero).lines()[1])
        assertTrue(!EscritorTrazados.escribirGpx(sendero).contains("<ele>"))
    }

    @Test
    fun `un circular cierra el trazado volviendo al primer punto`() {
        val sendero = crearSenderoDePrueba(recorrido = ida, tipoRecorrido = TipoRecorrido.CIRCULAR)

        val lineas = EscritorTrazados.escribirCsv(sendero).lines().filter { linea -> linea.isNotBlank() }

        assertEquals(lineas[1], lineas.last())
    }
}
