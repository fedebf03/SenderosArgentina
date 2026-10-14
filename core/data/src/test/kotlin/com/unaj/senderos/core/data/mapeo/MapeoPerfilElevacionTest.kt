package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.database.ConvertidoresSendero
import com.unaj.senderos.core.network.SenderoRed
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.data.falsos.crearEntidadDePrueba
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapeoPerfilElevacionTest {

    private val perfil = PerfilElevacion(
        puntos = listOf(PuntoPerfil(0.0, 1000.5), PuntoPerfil(0.05, 1004.0), PuntoPerfil(0.1, 1010.25)),
        desnivelPositivoMetros = 10,
        desnivelNegativoMetros = 2,
        alturaMaximaMetros = 1010,
        alturaMinimaMetros = 1000,
        pendienteMaximaPorcentaje = 12.5,
        firmaTrazado = "abc123"
    )

    @Test
    fun `el perfil hace ida y vuelta de dominio a entidad y de vuelta`() {
        val sendero = crearSenderoDePrueba(perfilElevacion = perfil)

        val entidad = sendero.aEntidad(1_000L, EstadoSincronizacion.PENDIENTE, false)

        assertEquals(perfil, entidad.aDominio().perfilElevacion)
    }

    @Test
    fun `el perfil hace ida y vuelta de entidad a red y de vuelta`() {
        val entidad = crearSenderoDePrueba(perfilElevacion = perfil).aEntidad(1_000L, EstadoSincronizacion.PENDIENTE, false)

        val red = entidad.aRed()
        val recuperada = red.aEntidad("usuario-1")

        assertEquals(3, red.perfilElevacion?.puntos?.size)
        assertEquals(perfil, recuperada.aDominio().perfilElevacion)
    }

    @Test
    fun `un sendero sin perfil queda sin perfil en entidad y en red`() {
        val entidad = crearEntidadDePrueba()

        assertNull(entidad.aDominio().perfilElevacion)
        assertNull(entidad.aRed().perfilElevacion)
    }

    @Test
    fun `un documento viejo sin el campo perfil se lee como sin calcular`() {
        val red = SenderoRed(id = "sendero-1", nombre = "Viejo")

        assertNull(red.perfilElevacion)
        assertNull(red.aEntidad("usuario-1").aDominio().perfilElevacion)
    }

    @Test
    fun `el convertidor de Room guarda el perfil como texto y acepta null`() {
        val convertidor = ConvertidoresSendero()
        val entidad = crearSenderoDePrueba(perfilElevacion = perfil).aEntidad(1_000L, EstadoSincronizacion.PENDIENTE, false)

        val texto = convertidor.perfilATexto(entidad.perfilElevacion)
        val recuperado = convertidor.textoAPerfil(texto)

        assertEquals(entidad.perfilElevacion, recuperado)
        assertNull(convertidor.perfilATexto(null))
        assertNull(convertidor.textoAPerfil(null))
    }
}
