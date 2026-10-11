package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.PuntoRecorridoEntidad
import com.unaj.senderos.data.remote.PuntoRecorridoRed
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.falsos.crearEntidadDePrueba
import com.unaj.senderos.falsos.crearRedDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapeoSenderoTest {

    @Test
    fun `ida y vuelta dominio entidad dominio conserva los datos`() {
        val original = crearSenderoDePrueba(
            esFavorito = true,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE
        )

        val entidad = original.aEntidad(
            fechaModificacion = 5_000L,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE,
            estaEliminado = false
        )
        val recuperado = entidad.aDominio()

        assertEquals(original, recuperado)
    }

    @Test
    fun `dominio a entidad aplica fecha estado y marca de eliminado recibidos`() {
        val entidad = crearSenderoDePrueba().aEntidad(
            fechaModificacion = 7_777L,
            estadoSincronizacion = EstadoSincronizacion.CON_ERROR,
            estaEliminado = true
        )

        assertEquals(7_777L, entidad.fechaModificacion)
        assertEquals("CON_ERROR", entidad.estadoSincronizacion)
        assertTrue(entidad.estaEliminado)
        assertEquals("MEDIA", entidad.dificultad)
        assertEquals("CIRCULAR", entidad.tipoRecorrido)
    }

    @Test
    fun `ida y vuelta entidad red entidad conserva los datos y queda SINCRONIZADO`() {
        val entidadOriginal = crearEntidadDePrueba(
            esFavorito = true,
            fechaModificacion = 9_000L,
            estadoSincronizacion = "PENDIENTE"
        )

        val recuperada = entidadOriginal.aRed().aEntidad(idUsuario = entidadOriginal.idUsuario)

        assertEquals(entidadOriginal.copy(estadoSincronizacion = "SINCRONIZADO"), recuperada)
    }

    @Test
    fun `estaEliminado y eliminado se corresponden en ambos sentidos`() {
        val redEliminada = crearEntidadDePrueba(estaEliminado = true).aRed()
        val redVigente = crearEntidadDePrueba(estaEliminado = false).aRed()
        val entidadDesdeEliminada = crearRedDePrueba(eliminado = true).aEntidad("usuario-1")
        val entidadDesdeVigente = crearRedDePrueba(eliminado = false).aEntidad("usuario-1")

        assertTrue(redEliminada.eliminado)
        assertFalse(redVigente.eliminado)
        assertTrue(entidadDesdeEliminada.estaEliminado)
        assertFalse(entidadDesdeVigente.estaEliminado)
    }

    @Test
    fun `red a entidad asigna el idUsuario recibido y estado SINCRONIZADO`() {
        val entidad = crearRedDePrueba(id = "remoto-1", fechaModificacion = 4_321L).aEntidad("usuario-9")

        assertEquals("usuario-9", entidad.idUsuario)
        assertEquals("remoto-1", entidad.id)
        assertEquals(4_321L, entidad.fechaModificacion)
        assertEquals("SINCRONIZADO", entidad.estadoSincronizacion)
    }

    @Test
    fun `enums desconocidos en la entidad caen en el valor por defecto`() {
        val entidad = crearEntidadDePrueba().copy(
            dificultad = "EXTREMA",
            tipoRecorrido = "ESPIRAL",
            estadoSincronizacion = "QUIEN_SABE"
        )

        val sendero = entidad.aDominio()

        assertEquals(Dificultad.MEDIA, sendero.dificultad)
        assertEquals(TipoRecorrido.LINEAL, sendero.tipoRecorrido)
        assertEquals(EstadoSincronizacion.SINCRONIZADO, sendero.estadoSincronizacion)
    }

    @Test
    fun `enums validos de la entidad se resuelven por nombre`() {
        val entidad = crearEntidadDePrueba().copy(
            dificultad = "ALTA",
            tipoRecorrido = "IDA_Y_VUELTA",
            estadoSincronizacion = "CON_ERROR"
        )

        val sendero = entidad.aDominio()

        assertEquals(Dificultad.ALTA, sendero.dificultad)
        assertEquals(TipoRecorrido.IDA_Y_VUELTA, sendero.tipoRecorrido)
        assertEquals(EstadoSincronizacion.CON_ERROR, sendero.estadoSincronizacion)
    }

    @Test
    fun `enums desconocidos en el documento remoto caen en el valor por defecto`() {
        val red = crearRedDePrueba().copy(dificultad = "EXTREMA", tipoRecorrido = "ESPIRAL")

        val sendero = red.aEntidad("usuario-1").aDominio()

        assertEquals(Dificultad.MEDIA, sendero.dificultad)
        assertEquals(TipoRecorrido.LINEAL, sendero.tipoRecorrido)
    }

    @Test
    fun `ida y vuelta dominio entidad dominio conserva el recorrido y su orden`() {
        val recorrido = listOf(
            PuntoRecorrido(-40.5, -71.3, TipoPunto.REFUGIO, "Refugio Norte"),
            PuntoRecorrido(-40.6, -71.4, TipoPunto.PASO, ""),
            PuntoRecorrido(-40.7, -71.2, TipoPunto.PELIGRO, "Cornisa")
        )
        val original = crearSenderoDePrueba(recorrido = recorrido)

        val entidad = original.aEntidad(
            fechaModificacion = 5_000L,
            estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
            estaEliminado = false
        )

        assertEquals(
            listOf("REFUGIO", "PASO", "PELIGRO"),
            entidad.recorrido.map { punto -> punto.tipo }
        )
        assertEquals(recorrido, entidad.aDominio().recorrido)
    }

    @Test
    fun `ida y vuelta entidad red entidad conserva el recorrido y su orden`() {
        val entidadOriginal = crearEntidadDePrueba(
            recorrido = listOf(
                PuntoRecorridoEntidad(-40.5, -71.3, "AGUA", "Arroyo"),
                PuntoRecorridoEntidad(-40.6, -71.4, "CAMPING", "Zona de acampe")
            )
        )

        val red = entidadOriginal.aRed()
        val recuperada = red.aEntidad(idUsuario = entidadOriginal.idUsuario)

        assertEquals(
            listOf(PuntoRecorridoRed(-40.5, -71.3, "AGUA", "Arroyo"), PuntoRecorridoRed(-40.6, -71.4, "CAMPING", "Zona de acampe")),
            red.recorrido
        )
        assertEquals(entidadOriginal.recorrido, recuperada.recorrido)
    }

    @Test
    fun `tipo de punto desconocido en la entidad cae en PASO`() {
        val entidad = crearEntidadDePrueba(
            recorrido = listOf(PuntoRecorridoEntidad(-40.5, -71.3, "VOLCAN", "Lanin"))
        )

        val sendero = entidad.aDominio()

        assertEquals(listOf(PuntoRecorrido(-40.5, -71.3, TipoPunto.PASO, "Lanin")), sendero.recorrido)
    }

    @Test
    fun `tipo de punto desconocido en el documento remoto cae en PASO`() {
        val red = crearRedDePrueba(
            recorrido = listOf(PuntoRecorridoRed(-40.5, -71.3, "VOLCAN", "Lanin"))
        )

        val sendero = red.aEntidad("usuario-1").aDominio()

        assertEquals(listOf(PuntoRecorrido(-40.5, -71.3, TipoPunto.PASO, "Lanin")), sendero.recorrido)
    }

    @Test
    fun `documento remoto sin recorrido produce recorrido vacio`() {
        val sendero = crearRedDePrueba().aEntidad("usuario-1").aDominio()

        assertTrue(sendero.recorrido.isEmpty())
    }
}
