package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.ConvertidoresSendero
import com.unaj.senderos.data.local.PuntoRecorridoEntidad
import com.unaj.senderos.data.remote.PuntoRecorridoRed
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.falsos.crearEntidadDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Test

class MapeoPuntoRecorridoFotoTest {

    private val puntoConFoto = PuntoRecorrido(-39.95, -71.07, TipoPunto.MIRADOR, "Mirador", foto = "/fotos/a.jpg")

    // Verificar que la foto del punto viaja del dominio a la entidad
    @Test
    fun `la foto del punto pasa del dominio a la entidad`() {
        val entidad = crearSenderoDePrueba(recorrido = listOf(puntoConFoto))
            .aEntidad(1_000L, EstadoSincronizacion.PENDIENTE, estaEliminado = false)

        assertEquals("/fotos/a.jpg", entidad.recorrido.single().foto)
    }

    // Verificar que la foto del punto vuelve de la entidad al dominio
    @Test
    fun `la foto del punto pasa de la entidad al dominio`() {
        val entidad = crearEntidadDePrueba().copy(
            recorrido = listOf(PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", "Mirador", "/fotos/a.jpg"))
        )

        assertEquals("/fotos/a.jpg", entidad.aDominio().recorrido.single().foto)
    }

    // Verificar que la foto del punto pasa de la entidad al documento de red
    @Test
    fun `la foto del punto pasa de la entidad a la red`() {
        val entidad = crearEntidadDePrueba().copy(
            recorrido = listOf(PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", "Mirador", "https://imagenes.ejemplo/a.jpg"))
        )

        assertEquals("https://imagenes.ejemplo/a.jpg", entidad.aRed().recorrido.single().foto)
    }

    // Verificar que la foto del punto pasa del documento de red a la entidad
    @Test
    fun `la foto del punto pasa de la red a la entidad`() {
        val red = crearEntidadDePrueba().aRed().copy(
            recorrido = listOf(PuntoRecorridoRed(-39.95, -71.07, "AGUA", "Arroyo", "https://imagenes.ejemplo/b.jpg"))
        )

        assertEquals("https://imagenes.ejemplo/b.jpg", red.aEntidad("usuario-1").recorrido.single().foto)
    }

    // Verificar que un documento remoto viejo sin foto queda con la foto vacía
    @Test
    fun `un punto de red sin foto tiene la foto vacia`() {
        assertEquals("", PuntoRecorridoRed().foto)
    }

    // Verificar que el JSON guardado antes de existir la foto se lee con la foto vacía
    @Test
    fun `el JSON viejo sin el campo foto se lee con la foto vacia`() {
        val jsonViejo = """[{"latitud":-39.95,"longitud":-71.07,"tipo":"PASO","nombre":"Cruce"}]"""

        val puntos = ConvertidoresSendero().textoAListaPuntos(jsonViejo)

        assertEquals("", puntos.single().foto)
        assertEquals("Cruce", puntos.single().nombre)
    }

    // Verificar que la foto sobrevive al viaje de ida y vuelta por el JSON de Room
    @Test
    fun `la foto sobrevive al JSON de ida y vuelta`() {
        val convertidores = ConvertidoresSendero()
        val original = listOf(PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", "Mirador", "/fotos/a.jpg"))

        val recuperados = convertidores.textoAListaPuntos(convertidores.listaPuntosATexto(original))

        assertEquals(original, recuperados)
    }

    // Verificar que el dominio sin foto la tiene vacía por defecto
    @Test
    fun `un punto de dominio sin foto la tiene vacia`() {
        assertEquals("", PuntoRecorrido(0.0, 0.0).foto)
    }
}
