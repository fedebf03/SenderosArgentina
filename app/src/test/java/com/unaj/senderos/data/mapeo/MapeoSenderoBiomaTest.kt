package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.remote.SenderoRed
import com.unaj.senderos.domain.model.Bioma
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.falsos.crearEntidadDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Test

class MapeoSenderoBiomaTest {

    @Test
    fun `el bioma hace ida y vuelta de dominio a entidad y de vuelta`() {
        Bioma.entries.forEach { bioma ->
            val sendero = crearSenderoDePrueba(bioma = bioma)

            val entidad = sendero.aEntidad(1_000L, EstadoSincronizacion.PENDIENTE, false)

            assertEquals(bioma.name, entidad.bioma)
            assertEquals(bioma, entidad.aDominio().bioma)
        }
    }

    @Test
    fun `el bioma hace ida y vuelta de entidad a red y de vuelta`() {
        val entidad = crearEntidadDePrueba(bioma = "PUNA")

        val red = entidad.aRed()
        val entidadRecuperada = red.aEntidad("usuario-1")

        assertEquals("PUNA", red.bioma)
        assertEquals("PUNA", entidadRecuperada.bioma)
        assertEquals(Bioma.PUNA, entidadRecuperada.aDominio().bioma)
    }

    @Test
    fun `un bioma desconocido en la entidad se resuelve como SIN_DEFINIR en el dominio`() {
        val entidad = crearEntidadDePrueba(bioma = "BIOMA_INEXISTENTE")

        assertEquals(Bioma.SIN_DEFINIR, entidad.aDominio().bioma)
    }

    @Test
    fun `un bioma desconocido en el documento remoto se guarda como SIN_DEFINIR`() {
        val red = SenderoRed(id = "sendero-1", bioma = "BIOMA_INEXISTENTE")

        assertEquals("SIN_DEFINIR", red.aEntidad("usuario-1").bioma)
    }

    @Test
    fun `un documento viejo sin el campo bioma queda como SIN_DEFINIR`() {
        val red = SenderoRed(id = "sendero-1", nombre = "Viejo")

        assertEquals("SIN_DEFINIR", red.bioma)
        assertEquals(Bioma.SIN_DEFINIR, red.aEntidad("usuario-1").aDominio().bioma)
    }
}
