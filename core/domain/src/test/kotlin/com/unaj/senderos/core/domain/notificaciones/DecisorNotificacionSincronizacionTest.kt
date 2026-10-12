package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.MensajeNotificacion
import com.unaj.senderos.domain.model.ResumenSincronizacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * Regla documentada para el caso con senderos guardados sin conexión Y senderos con error:
 * en un intento intermedio se informa lo subido (SenderosSubidos), porque el error todavía se reintentará;
 * en el último intento se prioriza el error (SenderosConError), porque ya no habrá más reintentos.
 */
class DecisorNotificacionSincronizacionTest {

    // Construir un resumen con los contadores indicados
    private fun crearResumen(guardadosSinConexion: Int = 0, conError: Int = 0, subidos: Int = 0): ResumenSincronizacion {
        return ResumenSincronizacion(
            senderosSubidos = subidos,
            senderosGuardadosSinConexion = guardadosSinConexion,
            senderosConError = conError,
            primerError = if (conError > 0) IllegalStateException("fallo") else null
        )
    }

    // Verificar que se notifica lo subido cuando hubo senderos guardados sin conexión
    @Test
    fun `con senderos guardados sin conexion notifica los senderos subidos`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(
            crearResumen(guardadosSinConexion = 3, subidos = 3),
            esUltimoIntento = false
        )

        assertEquals(MensajeNotificacion.SenderosSubidos(3), mensaje)
    }

    // Verificar que se notifica lo subido también en el último intento si no hubo errores
    @Test
    fun `con senderos guardados sin conexion y ultimo intento sin errores notifica los senderos subidos`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(
            crearResumen(guardadosSinConexion = 2, subidos = 2),
            esUltimoIntento = true
        )

        assertEquals(MensajeNotificacion.SenderosSubidos(2), mensaje)
    }

    // Verificar que un error se notifica solo en el último intento
    @Test
    fun `con errores en el ultimo intento notifica los senderos con error`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(crearResumen(conError = 2), esUltimoIntento = true)

        assertEquals(MensajeNotificacion.SenderosConError(2), mensaje)
    }

    // Verificar que un error no se notifica mientras queden reintentos
    @Test
    fun `con errores y reintentos pendientes no notifica`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(crearResumen(conError = 2), esUltimoIntento = false)

        assertNull(mensaje)
    }

    // Verificar que subir senderos recientes sin errores no genera notificación
    @Test
    fun `con senderos subidos recientemente y sin errores no notifica`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(crearResumen(subidos = 4), esUltimoIntento = true)

        assertNull(mensaje)
    }

    // Verificar que un resumen vacío no genera notificación
    @Test
    fun `con resumen vacio no notifica`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(crearResumen(), esUltimoIntento = false)

        assertNull(mensaje)
    }

    // Verificar que con ambos casos y un intento intermedio se informa lo subido
    @Test
    fun `con guardados y errores en un intento intermedio notifica los senderos subidos`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(
            crearResumen(guardadosSinConexion = 1, conError = 2, subidos = 1),
            esUltimoIntento = false
        )

        assertEquals(MensajeNotificacion.SenderosSubidos(1), mensaje)
    }

    // Verificar que con ambos casos y el último intento se prioriza el error
    @Test
    fun `con guardados y errores en el ultimo intento prioriza los senderos con error`() {
        val mensaje = DecisorNotificacionSincronizacion.decidir(
            crearResumen(guardadosSinConexion = 1, conError = 2, subidos = 1),
            esUltimoIntento = true
        )

        assertEquals(MensajeNotificacion.SenderosConError(2), mensaje)
    }
}
