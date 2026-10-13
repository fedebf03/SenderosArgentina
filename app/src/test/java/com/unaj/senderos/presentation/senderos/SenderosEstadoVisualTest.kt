package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderosEstadoVisualTest {

    private val huellaAndina = Sendero(
        id = "1", idUsuario = "usuario-1", nombre = "Huella Andina",
        region = "Mendoza", distanciaKilometros = 12.5, dificultad = Dificultad.ALTA
    )
    private val huellaNeuquina = Sendero(
        id = "2", idUsuario = "usuario-1", nombre = "Huella Neuquina",
        region = "Neuquén", distanciaKilometros = 8.0, dificultad = Dificultad.BAJA
    )

    private fun crearEstado(textoBusqueda: String): SenderosEstadoVisual {
        return SenderosEstadoVisual(
            senderos = listOf(huellaAndina, huellaNeuquina),
            textoBusqueda = textoBusqueda
        )
    }

    @Test
    fun `filtra los senderos por nombre`() {
        val estado = crearEstado("Andina")

        assertEquals(listOf(huellaAndina), estado.senderosFiltrados)
    }

    @Test
    fun `filtra los senderos por region`() {
        val estado = crearEstado("Neuqu")

        assertEquals(listOf(huellaNeuquina), estado.senderosFiltrados)
    }

    @Test
    fun `el filtro ignora mayusculas y espacios en los extremos`() {
        val estado = crearEstado("  hUeLLa aNdInA  ")

        assertEquals(listOf(huellaAndina), estado.senderosFiltrados)
    }

    @Test
    fun `texto vacio devuelve todos los senderos`() {
        val estado = crearEstado("")

        assertEquals(listOf(huellaAndina, huellaNeuquina), estado.senderosFiltrados)
    }

    @Test
    fun `sin coincidencias devuelve lista vacia`() {
        val estado = crearEstado("Inexistente")

        assertTrue(estado.senderosFiltrados.isEmpty())
    }
}
