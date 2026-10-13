package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.ConvertidoresSendero
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.falsos.crearEntidadRecorridoDePrueba
import com.unaj.senderos.falsos.crearRecorridoDePrueba
import com.unaj.senderos.falsos.crearRecorridoRedDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapeoRecorridoTest {

    @Test
    fun `el dominio se mapea a entidad con fecha y estado`() {
        val recorrido = crearRecorridoDePrueba(pasos = 4000, ritmoPasosPorMinuto = 98.5, esSimulado = true)

        val entidad = recorrido.aEntidad(fechaModificacion = 7_000L, estadoSincronizacion = EstadoSincronizacion.PENDIENTE)

        assertEquals("recorrido-1", entidad.id)
        assertEquals("usuario-1", entidad.idUsuario)
        assertEquals("sendero-1", entidad.idSendero)
        assertEquals("Cerro Catedral", entidad.nombreSendero)
        assertEquals(4000, entidad.pasos)
        assertEquals(98.5, entidad.ritmoPasosPorMinuto ?: -1.0, 0.0)
        assertEquals(true, entidad.esSimulado)
        assertEquals(7_000L, entidad.fechaModificacion)
        assertEquals("PENDIENTE", entidad.estadoSincronizacion)
        assertEquals(-39.96, entidad.trazadoCaminado[1].latitud, 0.0)
    }

    @Test
    fun `dominio entidad dominio conserva todos los datos`() {
        val recorrido = crearRecorridoDePrueba(pasos = 10, ritmoPasosPorMinuto = 90.0)

        val devuelto = recorrido.aEntidad(1L, EstadoSincronizacion.PENDIENTE).aDominio()

        assertEquals(recorrido, devuelto)
    }

    @Test
    fun `pasos y ritmo nulos se conservan`() {
        val devuelto = crearEntidadRecorridoDePrueba().aDominio()

        assertNull(devuelto.pasos)
        assertNull(devuelto.ritmoPasosPorMinuto)
    }

    @Test
    fun `un estado desconocido se resuelve como sincronizado`() {
        val devuelto = crearEntidadRecorridoDePrueba(estadoSincronizacion = "OTRO").aDominio()

        assertEquals(EstadoSincronizacion.SINCRONIZADO, devuelto.estadoSincronizacion)
    }

    @Test
    fun `la entidad se mapea a red`() {
        val red = crearEntidadRecorridoDePrueba(fechaModificacion = 5_000L).aRed()

        assertEquals("recorrido-1", red.id)
        assertEquals(5_000L, red.fechaModificacion)
        assertEquals(2, red.trazadoCaminado.size)
        assertEquals(-71.07, red.trazadoCaminado[0].longitud, 0.0)
    }

    @Test
    fun `la red se mapea a entidad sincronizada`() {
        val entidad = crearRecorridoRedDePrueba(fechaModificacion = 9_000L).aEntidad("usuario-2")

        assertEquals("usuario-2", entidad.idUsuario)
        assertEquals("SINCRONIZADO", entidad.estadoSincronizacion)
        assertEquals(9_000L, entidad.fechaModificacion)
        assertEquals(Coordenada(-39.95, -71.07), entidad.aDominio().trazadoCaminado[0])
    }

    @Test
    fun `entidad red entidad conserva el contenido`() {
        val original = crearEntidadRecorridoDePrueba(estadoSincronizacion = "SINCRONIZADO")

        val devuelta = original.aRed().aEntidad(original.idUsuario)

        assertEquals(original, devuelta)
    }

    @Test
    fun `el convertidor de Room conserva el trazado caminado`() {
        val convertidor = ConvertidoresSendero()
        val trazado = crearEntidadRecorridoDePrueba().trazadoCaminado

        val texto = convertidor.listaCoordenadasATexto(trazado)

        assertEquals(trazado, convertidor.textoAListaCoordenadas(texto))
        assertEquals("[]", convertidor.listaCoordenadasATexto(emptyList()))
    }
}
