package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.database.puntocapturado.PuntoCapturadoEntidad
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.testing.crearPuntoCapturadoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Test

class MapeoPuntoCapturadoTest {

    @Test
    fun `el punto capturado se mapea a entidad`() {
        val punto = crearPuntoCapturadoDePrueba(tipo = TipoPunto.AGUA, nota = "Arroyo", foto = "/fotos/a.jpg", instanteMilisegundos = 5_000L)

        val entidad = punto.aEntidad()

        assertEquals("punto-1", entidad.id)
        assertEquals("usuario-1", entidad.idUsuario)
        assertEquals("sendero-1", entidad.idSendero)
        assertEquals("AGUA", entidad.tipo)
        assertEquals("Arroyo", entidad.nota)
        assertEquals("/fotos/a.jpg", entidad.foto)
        assertEquals(5_000L, entidad.instanteMilisegundos)
    }

    @Test
    fun `la entidad se mapea de vuelta al dominio`() {
        val punto = crearPuntoCapturadoDePrueba(tipo = TipoPunto.REFUGIO, nota = "Refugio techado", foto = "/fotos/b.jpg")

        assertEquals(punto, punto.aEntidad().aDominio())
    }

    @Test
    fun `un tipo desconocido se resuelve como paso`() {
        val entidad = PuntoCapturadoEntidad("p", "u", "s", 0.0, 0.0, "INEXISTENTE", "", "", 1L)

        assertEquals(TipoPunto.PASO, entidad.aDominio().tipo)
    }
}
