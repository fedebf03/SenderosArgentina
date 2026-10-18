package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Importar trazados de otras aplicaciones (GPX o CSV): lectura, simplificación, tipo de recorrido y perfil con las alturas del archivo
class ImportacionTrazadoTest {

    // Generar una línea casi recta hacia el oeste con un leve zigzag de 1 m y altura que sube de 180 a 192 m, como la costanera de El Calafate
    private fun ida(cantidad: Int = 200): List<PuntoLeido> = List(cantidad) { indice ->
        PuntoLeido(
            latitud = -50.335 + (if (indice % 2 == 0) 0.0 else 0.00001),
            longitud = -72.27 - indice * 0.0002,
            altitudMetros = 180.0 + 12.0 * indice / (cantidad - 1)
        )
    }

    // Armar un CSV con encabezado a partir de puntos leídos
    private fun comoCsv(puntos: List<PuntoLeido>, encabezado: String = "Latitude,Longitude,Elevation"): String =
        (listOf(encabezado) + puntos.map { punto -> "${punto.latitud},${punto.longitud},${punto.altitudMetros}" }).joinToString("\n")

    @Test
    fun `lee un csv con encabezado de latitud longitud y altura`() {
        val leido = LectorTrazados.leer(comoCsv(ida(3)), "costanera.csv").getOrThrow()

        assertEquals(3, leido.puntos.size)
        assertEquals(-50.335, leido.puntos[0].latitud, 1e-9)
        assertEquals(180.0, leido.puntos[0].altitudMetros ?: Double.NaN, 1e-9)
    }

    @Test
    fun `lee un csv con punto y coma, coma decimal y encabezado en castellano`() {
        val texto = "latitud;longitud;altura\n-50,3350;-72,2693;187\n-50,3351;-72,2705;185"

        val leido = LectorTrazados.leer(texto, "datos.csv").getOrThrow()

        assertEquals(2, leido.puntos.size)
        assertEquals(-72.2705, leido.puntos[1].longitud, 1e-9)
        assertEquals(185.0, leido.puntos[1].altitudMetros ?: Double.NaN, 1e-9)
    }

    @Test
    fun `un csv sin altura deja las alturas vacias y saltea lineas invalidas`() {
        val texto = "lat,lon\n-50.1,-72.1\nesto no es un punto\n-50.2,-72.2\n-95,-72.3"

        val leido = LectorTrazados.leer(texto, "sin-altura.csv").getOrThrow()

        assertEquals(2, leido.puntos.size)
        assertNull(leido.puntos[0].altitudMetros)
    }

    @Test
    fun `lee un gpx con nombre, puntos de traza con altura y waypoints`() {
        val texto = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Prueba">
              <wpt lat="-50.3360" lon="-72.2738"><name>Mirador del lago</name></wpt>
              <trk><name>Costanera de El Calafate</name><trkseg>
                <trkpt lat="-50.33505" lon="-72.2693"><ele>187.0</ele></trkpt>
                <trkpt lat="-50.33517" lon="-72.27053"><ele>185.0</ele></trkpt>
                <trkpt lon="-72.27131" lat="-50.33533"/>
              </trkseg></trk>
            </gpx>
        """.trimIndent()

        val leido = LectorTrazados.leer(texto, "archivo.gpx").getOrThrow()

        assertEquals("Costanera de El Calafate", leido.nombre)
        assertEquals(3, leido.puntos.size)
        assertEquals(-72.27131, leido.puntos[2].longitud, 1e-9)
        assertNull(leido.puntos[2].altitudMetros)
        assertEquals(listOf(PuntoDeInteresLeido(-50.3360, -72.2738, "Mirador del lago")), leido.puntosDeInteres)
    }

    @Test
    fun `un gpx de ruta sin traza usa los puntos de la ruta`() {
        val texto = """<gpx><rte><rtept lat="-41.1" lon="-71.3"/><rtept lat="-41.2" lon="-71.4"/></rte></gpx>"""

        assertEquals(2, LectorTrazados.leer(texto, "ruta.gpx").getOrThrow().puntos.size)
    }

    @Test
    fun `un archivo sin puntos validos falla con un mensaje claro`() {
        val resultado = LectorTrazados.leer("hola,mundo\nsin,numeros", "vacio.csv")

        assertEquals("El archivo no tiene puntos de recorrido válidos", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `simplificar quita los puntos que no cambian la forma y conserva los extremos`() {
        val puntos = ida().map { punto -> Coordenada(punto.latitud, punto.longitud) }

        val conservados = SimplificadorTrazado.simplificar(puntos, toleranciaMetros = 5.0)

        assertEquals(0, conservados.first())
        assertEquals(puntos.lastIndex, conservados.last())
        assertTrue("quedaron ${conservados.size} puntos", conservados.size <= 5)
    }

    @Test
    fun `simplificar conserva una esquina`() {
        val puntos = List(20) { indice -> Coordenada(-50.0, -72.0 - indice * 0.001) } +
            List(20) { indice -> Coordenada(-50.0 - (indice + 1) * 0.001, -72.019) }

        val conservados = SimplificadorTrazado.simplificar(puntos, toleranciaMetros = 5.0)

        assertTrue(19 in conservados)
    }

    @Test
    fun `una ida y vuelta por el mismo camino se importa como ida y vuelta con solo la ida`() {
        val leido = TrazadoLeido(nombre = null, puntos = ida() + ida().reversed().drop(1), puntosDeInteres = emptyList())

        val importado = ImportadorTrazado.importar(leido, "Costanera_de_El_Calafate.csv").getOrThrow()

        assertEquals(TipoRecorrido.IDA_Y_VUELTA, importado.tipoRecorrido)
        assertEquals(-72.27, importado.recorrido.first().longitud, 1e-6)
        assertEquals(-72.27 - 199 * 0.0002, importado.recorrido.last().longitud, 1e-6)
        assertEquals("Costanera de El Calafate", importado.nombre)
    }

    @Test
    fun `una vuelta que termina donde empieza se importa como circular sin repetir el inicio`() {
        val vuelta = List(30) { indice -> PuntoLeido(-50.0, -72.0 - indice * 0.001, null) } +
            List(30) { indice -> PuntoLeido(-50.0 - (indice + 1) * 0.001, -72.029, null) } +
            List(30) { indice -> PuntoLeido(-50.03, -72.029 + (indice + 1) * 0.001, null) } +
            List(30) { indice -> PuntoLeido(-50.03 + (indice + 1) * 0.001, -71.999, null) }

        val importado = ImportadorTrazado.importar(TrazadoLeido(null, vuelta, emptyList()), "vuelta.gpx").getOrThrow()

        assertEquals(TipoRecorrido.CIRCULAR, importado.tipoRecorrido)
        assertTrue(importado.recorrido.size in 4..6)
    }

    @Test
    fun `un camino que no vuelve se importa como lineal`() {
        val importado = ImportadorTrazado.importar(TrazadoLeido(null, ida(), emptyList()), "lineal.csv").getOrThrow()

        assertEquals(TipoRecorrido.LINEAL, importado.tipoRecorrido)
    }

    @Test
    fun `con las alturas del archivo arma el perfil sin consultar la red`() {
        val leido = TrazadoLeido(nombre = null, puntos = ida() + ida().reversed().drop(1), puntosDeInteres = emptyList())

        val perfil = ImportadorTrazado.importar(leido, "costanera.csv").getOrThrow().perfil

        assertNotNull(perfil)
        assertEquals(192, perfil?.alturaMaximaMetros)
        assertEquals(180, perfil?.alturaMinimaMetros)
    }

    @Test
    fun `sin alturas en el archivo no hay perfil`() {
        val sinAltura = ida().map { punto -> punto.copy(altitudMetros = null) }

        assertNull(ImportadorTrazado.importar(TrazadoLeido(null, sinAltura, emptyList()), "x.csv").getOrThrow().perfil)
    }

    @Test
    fun `los waypoints del gpx se agregan como puntos de interes de tipo otro en el tramo mas cercano`() {
        val mirador = PuntoDeInteresLeido(-50.3352, -72.29, "Mirador")

        val importado = ImportadorTrazado.importar(TrazadoLeido(null, ida(), listOf(mirador)), "x.gpx").getOrThrow()

        val punto = importado.recorrido.single { candidato -> candidato.tipo == TipoPunto.OTRO }
        assertEquals("Mirador", punto.nombre)
        assertTrue(importado.recorrido.indexOf(punto) in 1 until importado.recorrido.lastIndex)
    }

    @Test
    fun `un waypoint lejos del camino no se agrega`() {
        val lejano = PuntoDeInteresLeido(-50.40, -72.29, "Otro cerro")

        val importado = ImportadorTrazado.importar(TrazadoLeido(null, ida(), listOf(lejano)), "x.gpx").getOrThrow()

        assertTrue(importado.recorrido.none { punto -> punto.tipo == TipoPunto.OTRO })
    }

    // Miles de aperturas sin cerrar no deben volver lenta la lectura
    @Test(timeout = 5_000)
    fun `un gpx con etiquetas sin cerrar se lee rapido`() {
        val texto = "<gpx><trk><trkseg>" + "<trkpt lat=\"-41.1\" lon=\"-71.3\">".repeat(50_000) + "</trkseg></trk></gpx>"

        val leido = LectorTrazados.leer(texto, "roto.gpx").getOrThrow()

        assertEquals(50_000, leido.puntos.size)
    }

    @Test
    fun `un archivo con mas de cien mil puntos se rechaza`() {
        val texto = "lat,lon\n" + "-41.1,-71.3\n".repeat(MAXIMO_PUNTOS_LEIDOS + 1)

        assertEquals(
            "El archivo tiene demasiados puntos para un sendero (más de 100.000)",
            LectorTrazados.leer(texto, "enorme.csv").exceptionOrNull()?.message
        )
    }

    @Test
    fun `un trazado de un solo punto no se puede importar`() {
        val resultado = ImportadorTrazado.importar(TrazadoLeido(null, ida(1), emptyList()), "x.csv")

        assertEquals("El archivo necesita al menos dos puntos distintos", resultado.exceptionOrNull()?.message)
    }
}
