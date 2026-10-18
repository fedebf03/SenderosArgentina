package com.unaj.senderos.features.senderos.listado

import com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion
import org.junit.Assert.assertEquals
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
}
