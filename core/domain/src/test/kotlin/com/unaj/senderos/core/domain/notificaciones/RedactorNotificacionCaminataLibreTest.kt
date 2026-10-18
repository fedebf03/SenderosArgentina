package com.unaj.senderos.core.domain.notificaciones

import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Notificación persistente de una caminata libre: no hay sendero, así que no hay restante ni porcentaje de avance
class RedactorNotificacionCaminataLibreTest {

    private fun estadoLibre(
        calidad: CalidadSenal = CalidadSenal.BUENA,
        kilometros: Double = 1.25,
        pasos: Int? = 1800
    ) = EstadoSeguimiento(
        idSendero = "libre-1000",
        trazado = emptyList(),
        nombreSendero = "Caminata libre",
        distanciaRecorridaKilometros = kilometros,
        calidadSenal = calidad,
        instanteInicioMilisegundos = 1_000L,
        esCaminataLibre = true,
        pasos = pasos
    )

    @Test
    fun `el titulo dice caminata libre y no el nombre de un sendero`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(estadoLibre(), esSimulado = false)

        assertEquals("Caminata libre", contenido.titulo)
    }

    @Test
    fun `el texto corto informa solo los kilometros recorridos`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(estadoLibre(kilometros = 1.25), esSimulado = false)

        assertEquals("1,3 km recorridos", contenido.textoCorto)
        assertFalse(contenido.textoCorto.contains("restantes"))
    }

    @Test
    fun `no muestra barra de progreso`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(estadoLibre(), esSimulado = false)

        assertFalse(contenido.tieneProgreso)
    }

    @Test
    fun `un recorrido por sendero sigue mostrando su barra de progreso`() {
        val estado = estadoLibre().copy(esCaminataLibre = false, nombreSendero = "Huella Andina")

        val contenido = RedactorNotificacionRecorrido.redactarPersistente(estado, esSimulado = false)

        assertTrue(contenido.tieneProgreso)
        assertEquals("Recorriendo Huella Andina", contenido.titulo)
    }

    @Test
    fun `con la senal debil o buscando conserva los mismos mensajes que un recorrido normal`() {
        val debil = RedactorNotificacionRecorrido.redactarPersistente(estadoLibre(CalidadSenal.DEBIL), esSimulado = false)
        val buscando = RedactorNotificacionRecorrido.redactarPersistente(estadoLibre(CalidadSenal.BUSCANDO), esSimulado = false)

        assertEquals("Sin señal GPS · salí al aire libre", debil.textoCorto)
        assertEquals("Buscando señal GPS…", buscando.textoCorto)
    }

    @Test
    fun `el texto expandido agrega velocidad y pasos y nunca un proximo punto`() {
        val contenido = RedactorNotificacionRecorrido.redactarPersistente(estadoLibre(pasos = 1800), esSimulado = false)

        assertTrue(contenido.textoExpandido.contains("Pasos: 1800"))
        assertFalse(contenido.textoExpandido.contains("Próximo punto"))
    }
}
