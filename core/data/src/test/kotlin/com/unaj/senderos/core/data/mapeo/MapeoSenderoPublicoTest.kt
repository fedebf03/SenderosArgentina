package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import com.unaj.senderos.core.testing.crearSenderoPublicoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MapeoSenderoPublicoTest {

    private val recorrido = listOf(
        PuntoRecorrido(-41.10, -71.30, TipoPunto.PASO),
        PuntoRecorrido(-41.11, -71.31, TipoPunto.MIRADOR, nombre = "Mirador", foto = "https://res.cloudinary.com/demo/m.jpg")
    )

    private fun publicoDePrueba() = crearSenderoPublicoDePrueba(
        sendero = crearSenderoDePrueba(id = "publico-1", idUsuario = "autor-1", esFavorito = true, recorrido = recorrido)
            .copy(foto = "https://res.cloudinary.com/demo/s.jpg"),
        nombreAutor = "Luis",
        fechaPublicacion = 5_000L
    )

    @Test
    fun `el documento remoto lleva al autor y la fecha de publicacion`() {
        val red = publicoDePrueba().aRed()

        assertEquals("autor-1", red.idAutor)
        assertEquals("Luis", red.nombreAutor)
        assertEquals(5_000L, red.fechaPublicacion)
        assertEquals("publico-1", red.sendero.id)
    }

    @Test
    fun `el documento remoto nunca lleva la marca de favorito`() {
        assertFalse(publicoDePrueba().aRed().sendero.esFavorito)
    }

    @Test
    fun `ida y vuelta conserva el recorrido, la foto y los datos`() {
        val original = publicoDePrueba()

        val restaurado = original.aRed().aDominio()

        assertEquals(original.sendero.copy(esFavorito = false), restaurado.sendero)
        assertEquals(original.nombreAutor, restaurado.nombreAutor)
        assertEquals(original.fechaPublicacion, restaurado.fechaPublicacion)
    }

    @Test
    fun `lo que llega de la nube figura como sincronizado y a nombre del autor`() {
        val restaurado = publicoDePrueba().aRed().aDominio()

        assertEquals(EstadoSincronizacion.SINCRONIZADO, restaurado.sendero.estadoSincronizacion)
        assertEquals("autor-1", restaurado.idAutor)
    }
}
