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

    // Crear un estado visual con los dos senderos y el texto de búsqueda indicado
    private fun crearEstado(textoBusqueda: String): SenderosEstadoVisual {
        return SenderosEstadoVisual(
            senderos = listOf(huellaAndina, huellaNeuquina),
            textoBusqueda = textoBusqueda
        )
    }

    // Probar que filtra los senderos por coincidencia en el nombre
    @Test
    fun `filtra los senderos por nombre`() {
        val estado = crearEstado("Andina")

        assertEquals(listOf(huellaAndina), estado.senderosFiltrados)
    }

    // Probar que filtra los senderos por coincidencia en la región
    @Test
    fun `filtra los senderos por region`() {
        val estado = crearEstado("Neuqu")

        assertEquals(listOf(huellaNeuquina), estado.senderosFiltrados)
    }

    // Probar que el filtro ignora mayúsculas y espacios al inicio y al final
    @Test
    fun `el filtro ignora mayusculas y espacios en los extremos`() {
        val estado = crearEstado("  hUeLLa aNdInA  ")

        assertEquals(listOf(huellaAndina), estado.senderosFiltrados)
    }

    // Probar que un texto vacío devuelve todos los senderos
    @Test
    fun `texto vacio devuelve todos los senderos`() {
        val estado = crearEstado("")

        assertEquals(listOf(huellaAndina, huellaNeuquina), estado.senderosFiltrados)
    }

    // Probar que un texto sin coincidencias devuelve una lista vacía
    @Test
    fun `sin coincidencias devuelve lista vacia`() {
        val estado = crearEstado("Inexistente")

        assertTrue(estado.senderosFiltrados.isEmpty())
    }
}
