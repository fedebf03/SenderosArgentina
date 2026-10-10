package com.unaj.senderos.data.ejemplos

import com.unaj.senderos.domain.model.TipoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderosDeEjemploTest {

    // Probar que se crean los dos senderos de ejemplo con el idUsuario recibido
    @Test
    fun `crea Huella Andina y Huella Neuquina con el idUsuario recibido`() {
        val senderos = crearSenderosDeEjemplo("usuario-7")

        assertEquals(2, senderos.size)
        assertEquals(
            setOf("Huella Andina", "Huella Neuquina"),
            senderos.map { sendero -> sendero.nombre }.toSet()
        )
        assertTrue(senderos.all { sendero -> sendero.idUsuario == "usuario-7" })
    }

    // Probar que Huella Andina trae sus datos de detalle completos
    @Test
    fun `Huella Andina trae sus datos de detalle`() {
        val sendero = crearSenderosDeEjemplo("usuario-1").first { elemento -> elemento.id == "huella-andina" }

        assertEquals("Huella Andina", sendero.nombre)
        assertEquals(570.0, sendero.distanciaKilometros, 0.0)
        assertEquals(1200, sendero.desnivelMetros)
        assertEquals(TipoRecorrido.LINEAL, sendero.tipoRecorrido)
        assertEquals(3, sendero.etapas.size)
        assertEquals(3, sendero.recomendaciones.size)
        assertFalse(sendero.esFavorito)
    }

    // Probar que Huella Neuquina trae sus datos de detalle completos
    @Test
    fun `Huella Neuquina trae sus datos de detalle`() {
        val sendero = crearSenderosDeEjemplo("usuario-1").first { elemento -> elemento.id == "huella-neuquina" }

        assertEquals("Huella Neuquina", sendero.nombre)
        assertEquals(120.0, sendero.distanciaKilometros, 0.0)
        assertEquals(TipoRecorrido.IDA_Y_VUELTA, sendero.tipoRecorrido)
        assertEquals(2, sendero.etapas.size)
        assertEquals(2, sendero.servicios.size)
    }

    // Probar que los identificadores de los ejemplos son estables entre llamadas
    @Test
    fun `los identificadores de los ejemplos son estables`() {
        val identificadores = crearSenderosDeEjemplo("usuario-1").map { sendero -> sendero.id }

        assertEquals(listOf("huella-andina", "huella-neuquina"), identificadores)
        assertEquals(identificadores, crearSenderosDeEjemplo("usuario-2").map { sendero -> sendero.id })
    }
}
