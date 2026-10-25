package com.unaj.senderos.core.ui.comun

import org.junit.Assert.assertEquals
import org.junit.Test

class MensajesDeErrorTest {

    @Test
    fun `un error de dominio muestra su mensaje`() {
        assertEquals("Sin posición", describirErrorParaElUsuario(IllegalStateException("Sin posición"), "Respaldo"))
        assertEquals("Dato inválido", describirErrorParaElUsuario(IllegalArgumentException("Dato inválido"), "Respaldo"))
    }

    @Test
    fun `un error tecnico o sin mensaje usa el respaldo`() {
        assertEquals("Respaldo", describirErrorParaElUsuario(RuntimeException("SQLite boom"), "Respaldo"))
        assertEquals("Respaldo", describirErrorParaElUsuario(IllegalStateException("  "), "Respaldo"))
        assertEquals("Respaldo", describirErrorParaElUsuario(IllegalStateException(), "Respaldo"))
    }
}
