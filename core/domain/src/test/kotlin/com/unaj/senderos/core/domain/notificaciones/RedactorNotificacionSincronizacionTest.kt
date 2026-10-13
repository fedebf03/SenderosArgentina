package com.unaj.senderos.domain.notificaciones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactorNotificacionSincronizacionTest {

    @Test
    fun `un sendero subido se redacta en singular sin reintento`() {
        val contenido = RedactorNotificacionSincronizacion.redactar(MensajeNotificacion.SenderosSubidos(1))
        assertEquals("Senderos sincronizados", contenido.titulo)
        assertEquals("Tu sendero guardado sin señal ya está en la nube", contenido.texto)
        assertFalse(contenido.permiteReintentar)
    }

    @Test
    fun `varios senderos subidos se redactan en plural`() {
        val contenido = RedactorNotificacionSincronizacion.redactar(MensajeNotificacion.SenderosSubidos(3))
        assertEquals("Tus 3 senderos guardados sin señal ya están en la nube", contenido.texto)
    }

    @Test
    fun `el error permite reintentar`() {
        val contenido = RedactorNotificacionSincronizacion.redactar(MensajeNotificacion.SenderosConError(2))
        assertEquals("Error de sincronización", contenido.titulo)
        assertEquals(
            "No se pudieron sincronizar 2 cambios (senderos o recorridos). Se reintentará al abrir la app",
            contenido.texto
        )
        assertTrue(contenido.permiteReintentar)
    }

    @Test
    fun `un error se redacta en singular`() {
        val contenido = RedactorNotificacionSincronizacion.redactar(MensajeNotificacion.SenderosConError(1))
        assertEquals("No se pudo sincronizar 1 cambio (sendero o recorrido). Se reintentará al abrir la app", contenido.texto)
    }
}
