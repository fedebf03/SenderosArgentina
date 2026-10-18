package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Copia de seguridad en JSON: conserva todos los datos que el usuario cargó y es tolerante con archivos de otras versiones
class CopiaSeguridadJsonTest {

    private val formato = CopiaSeguridadJson()

    private val sendero = crearSenderoDePrueba(
        id = "s1",
        nombre = "Cerro Otto",
        dificultad = Dificultad.ALTA,
        bioma = Bioma.BOSQUE_ANDINO_PATAGONICO,
        tipoRecorrido = TipoRecorrido.IDA_Y_VUELTA,
        recorrido = listOf(
            PuntoRecorrido(-41.10, -71.30),
            PuntoRecorrido(-41.11, -71.31, tipo = TipoPunto.MIRADOR, nombre = "Vista", foto = "https://res.cloudinary.com/x/foto.jpg"),
            PuntoRecorrido(-41.12, -71.33, tipo = TipoPunto.AGUA, foto = "/data/user/0/app/files/fotos_senderos/a.jpg")
        )
    ).copy(
        duracionEstimada = "3 h",
        puntoInicio = "Estacionamiento",
        mejorEpoca = "Verano",
        recomendaciones = listOf("Llevar agua"),
        servicios = listOf("Baños"),
        etapas = listOf(Etapa(1, "Subida", 2.5)),
        esFavorito = true,
        foto = "https://res.cloudinary.com/x/portada.jpg"
    )

    @Test
    fun `escribir y leer conserva los datos del sendero`() {
        val leido = formato.leer(formato.escribir(listOf(sendero), 1_000L)).getOrThrow().single()

        assertEquals("Cerro Otto", leido.nombre)
        assertEquals(Dificultad.ALTA, leido.dificultad)
        assertEquals(Bioma.BOSQUE_ANDINO_PATAGONICO, leido.bioma)
        assertEquals(TipoRecorrido.IDA_Y_VUELTA, leido.tipoRecorrido)
        assertEquals(listOf(Etapa(1, "Subida", 2.5)), leido.etapas)
        assertEquals(listOf("Llevar agua"), leido.recomendaciones)
        assertEquals("Vista", leido.recorrido[1].nombre)
        assertEquals(TipoPunto.MIRADOR, leido.recorrido[1].tipo)
        assertTrue(leido.esFavorito)
        assertEquals("https://res.cloudinary.com/x/portada.jpg", leido.foto)
    }

    // Una ruta local no sirve en otro teléfono: solo viajan las fotos ya subidas a la nube
    @Test
    fun `las fotos locales no viajan en la copia`() {
        val leido = formato.leer(formato.escribir(listOf(sendero), 1_000L)).getOrThrow().single()

        assertEquals("https://res.cloudinary.com/x/foto.jpg", leido.recorrido[1].foto)
        assertEquals("", leido.recorrido[2].foto)
    }

    // Una copia armada a mano no puede hacer que la app suba un archivo privado del teléfono ni muestre sitios de terceros
    @Test
    fun `al leer se descartan las fotos que no son de la nube de la app`() {
        val texto = formato.escribir(listOf(sendero), 1_000L)
            .replace("https://res.cloudinary.com/x/portada.jpg", "/data/user/0/app/files/privado.jpg")
            .replace("https://res.cloudinary.com/x/foto.jpg", "https://otro-sitio.com/foto.jpg")

        val leido = formato.leer(texto).getOrThrow().single()

        assertEquals("", leido.foto)
        assertEquals("", leido.recorrido[1].foto)
    }

    @Test
    fun `un id con un formato inesperado se lee vacio para guardarse como sendero nuevo`() {
        val texto = formato.escribir(listOf(sendero.copy(id = "s1")), 1_000L).replace("\"s1\"", "\"usuarios/otro/senderos/x\"")

        assertEquals("", formato.leer(texto).getOrThrow().single().id)
    }

    @Test
    fun `el archivo indica la aplicacion y la version del formato`() {
        val texto = formato.escribir(listOf(sendero), 1_000L)

        assertTrue(texto.contains("\"aplicacion\""))
        assertTrue(texto.contains("\"version\": 1"))
    }

    @Test
    fun `un valor desconocido de otra version se lee con el valor por defecto`() {
        val texto = formato.escribir(listOf(sendero), 1_000L)
            .replace("\"BOSQUE_ANDINO_PATAGONICO\"", "\"SELVA_FUTURA\"")
            .replace("\"ALTA\"", "\"EXTREMA\"")

        val leido = formato.leer(texto).getOrThrow().single()

        assertEquals(Bioma.SIN_DEFINIR, leido.bioma)
        assertEquals(Dificultad.MEDIA, leido.dificultad)
    }

    @Test
    fun `un texto que no es una copia de senderos falla con un mensaje claro`() {
        val mensaje = "El archivo no es una copia de seguridad de Senderos"

        assertEquals(mensaje, formato.leer("{\"otra\": 1}").exceptionOrNull()?.message)
        assertEquals(mensaje, formato.leer("no es json").exceptionOrNull()?.message)
    }
}
