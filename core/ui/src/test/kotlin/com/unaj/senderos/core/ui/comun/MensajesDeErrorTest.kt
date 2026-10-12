package com.unaj.senderos.presentation.comun

import org.junit.Assert.assertEquals
import org.junit.Test

class MensajesDeErrorTest {

    // Verificar que un error de dominio con mensaje en español se muestra tal cual
    @Test
    fun `un error de dominio muestra su mensaje`() {
        assertEquals("Sin posición", describirErrorParaElUsuario(IllegalStateException("Sin posición"), "Respaldo"))
        assertEquals("Dato inválido", describirErrorParaElUsuario(IllegalArgumentException("Dato inválido"), "Respaldo"))
    }

    // Verificar que un error técnico o sin mensaje usa el texto de respaldo
    @Test
    fun `un error tecnico o sin mensaje usa el respaldo`() {
        assertEquals("Respaldo", describirErrorParaElUsuario(RuntimeException("SQLite boom"), "Respaldo"))
        assertEquals("Respaldo", describirErrorParaElUsuario(IllegalStateException("  "), "Respaldo"))
        assertEquals("Respaldo", describirErrorParaElUsuario(IllegalStateException(), "Respaldo"))
    }
}
