package com.unaj.senderos.features.senderos.listado

import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// El texto de sincronización no debe culpar a la conexión cuando hay red
class DescripcionSincronizacionTest {

    @Test
    fun `en cola con conexion no dice que espera conexion`() {
        val estado = SenderosEstadoVisual(estadoTrabajoSincronizacion = EstadoTrabajoSincronizacion.EN_COLA, estaSinConexion = false)

        assertEquals("Sincronización en cola…", describirSincronizacion(estado))
    }

    @Test
    fun `en cola sin conexion dice que espera conexion`() {
        val estado = SenderosEstadoVisual(estadoTrabajoSincronizacion = EstadoTrabajoSincronizacion.EN_COLA, estaSinConexion = true)

        assertEquals("Esperando conexión para sincronizar", describirSincronizacion(estado))
    }

    @Test
    fun `tras un intento fallido avisa que se reintenta`() {
        val estado = SenderosEstadoVisual(estadoTrabajoSincronizacion = EstadoTrabajoSincronizacion.EN_REINTENTO)

        assertEquals("Falló el último intento; se reintenta solo en unos instantes", describirSincronizacion(estado))
    }

    @Test
    fun `con todo subido y nada en marcha no hay resumen`() {
        val estado = SenderosEstadoVisual()

        assertFalse(hayAlgoPorSincronizar(estado))
        assertFalse(sePuedeReintentarSincronizacion(estado))
    }

    @Test
    fun `con senderos sin subir hay resumen y se puede reintentar`() {
        val estado = SenderosEstadoVisual(senderos = listOf(sendero(EstadoSincronizacion.PENDIENTE), sendero(EstadoSincronizacion.PENDIENTE)))

        assertTrue(hayAlgoPorSincronizar(estado))
        assertTrue(sePuedeReintentarSincronizacion(estado))
    }

    @Test
    fun `con error de subida hay resumen y se puede reintentar`() {
        val estado = SenderosEstadoVisual(senderos = listOf(sendero(EstadoSincronizacion.CON_ERROR)))

        assertTrue(hayAlgoPorSincronizar(estado))
        assertTrue(sePuedeReintentarSincronizacion(estado))
    }

    @Test
    fun `mientras sincroniza hay resumen pero sin boton`() {
        val estado = SenderosEstadoVisual(estadoTrabajoSincronizacion = EstadoTrabajoSincronizacion.EN_CURSO, senderos = listOf(sendero(EstadoSincronizacion.PENDIENTE)))

        assertTrue(hayAlgoPorSincronizar(estado))
        assertFalse(sePuedeReintentarSincronizacion(estado))
    }

    @Test
    fun `en cola sin conexion hay resumen pero sin boton`() {
        val estado = SenderosEstadoVisual(estadoTrabajoSincronizacion = EstadoTrabajoSincronizacion.EN_COLA, estaSinConexion = true)

        assertTrue(hayAlgoPorSincronizar(estado))
        assertFalse(sePuedeReintentarSincronizacion(estado))
    }

    private fun sendero(estado: EstadoSincronizacion) = crearSenderoDePrueba(estadoSincronizacion = estado)
}
