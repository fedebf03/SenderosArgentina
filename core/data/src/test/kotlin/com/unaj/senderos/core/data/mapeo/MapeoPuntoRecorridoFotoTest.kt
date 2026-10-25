package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.database.ConvertidoresSendero
import com.unaj.senderos.core.database.sendero.PuntoRecorridoEntidad
import com.unaj.senderos.core.network.sendero.PuntoRecorridoRed
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.data.falsos.crearEntidadDePrueba
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Test

class MapeoPuntoRecorridoFotoTest {

    private val puntoConFoto = PuntoRecorrido(-39.95, -71.07, TipoPunto.MIRADOR, "Mirador", foto = "/fotos/a.jpg")

    @Test
    fun `la foto del punto pasa del dominio a la entidad`() {
        val entidad = crearSenderoDePrueba(recorrido = listOf(puntoConFoto))
            .aEntidad(1_000L, EstadoSincronizacion.PENDIENTE, estaEliminado = false)

        assertEquals("/fotos/a.jpg", entidad.recorrido.single().foto)
    }

    @Test
    fun `la foto del punto pasa de la entidad al dominio`() {
        val entidad = crearEntidadDePrueba().copy(
            recorrido = listOf(PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", "Mirador", "/fotos/a.jpg"))
        )

        assertEquals("/fotos/a.jpg", entidad.aDominio().recorrido.single().foto)
    }

    @Test
    fun `la foto del punto pasa de la entidad a la red`() {
        val entidad = crearEntidadDePrueba().copy(
            recorrido = listOf(PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", "Mirador", "https://imagenes.ejemplo/a.jpg"))
        )

        assertEquals("https://imagenes.ejemplo/a.jpg", entidad.aRed().recorrido.single().foto)
    }

    @Test
    fun `la foto del punto pasa de la red a la entidad`() {
        val red = crearEntidadDePrueba().aRed().copy(
            recorrido = listOf(PuntoRecorridoRed(-39.95, -71.07, "AGUA", "Arroyo", "https://imagenes.ejemplo/b.jpg"))
        )

        assertEquals("https://imagenes.ejemplo/b.jpg", red.aEntidad("usuario-1").recorrido.single().foto)
    }

    // Un documento remoto viejo sin foto queda con la foto vacía
    @Test
    fun `un punto de red sin foto tiene la foto vacia`() {
        assertEquals("", PuntoRecorridoRed().foto)
    }

    @Test
    fun `el JSON viejo sin el campo foto se lee con la foto vacia`() {
        val jsonViejo = """[{"latitud":-39.95,"longitud":-71.07,"tipo":"PASO","nombre":"Cruce"}]"""

        val puntos = ConvertidoresSendero().textoAListaPuntos(jsonViejo)

        assertEquals("", puntos.single().foto)
        assertEquals("Cruce", puntos.single().nombre)
    }

    @Test
    fun `la foto sobrevive al JSON de ida y vuelta`() {
        val convertidores = ConvertidoresSendero()
        val original = listOf(PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", "Mirador", "/fotos/a.jpg"))

        val recuperados = convertidores.textoAListaPuntos(convertidores.listaPuntosATexto(original))

        assertEquals(original, recuperados)
    }

    @Test
    fun `un punto de dominio sin foto la tiene vacia`() {
        assertEquals("", PuntoRecorrido(0.0, 0.0).foto)
    }
}
