package com.unaj.senderos.domain.model

import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroSenderosTest {

    private val facil = crearSenderoDePrueba(id = "facil", dificultad = Dificultad.BAJA)
    private val intermedio = crearSenderoDePrueba(id = "intermedio", dificultad = Dificultad.MEDIA)
    private val exigente = crearSenderoDePrueba(id = "exigente", dificultad = Dificultad.ALTA)
    private val todos = listOf(facil, intermedio, exigente)

    @Test
    fun `sin dificultades seleccionadas devuelve todos los senderos`() {
        assertEquals(todos, filtrarSenderosPorDificultad(todos, emptySet()))
    }

    @Test
    fun `con una dificultad devuelve solo los senderos de esa dificultad`() {
        assertEquals(listOf(intermedio), filtrarSenderosPorDificultad(todos, setOf(Dificultad.MEDIA)))
    }

    @Test
    fun `con varias dificultades devuelve los que coinciden con alguna conservando el orden`() {
        val resultado = filtrarSenderosPorDificultad(todos, setOf(Dificultad.ALTA, Dificultad.BAJA))

        assertEquals(listOf(facil, exigente), resultado)
    }

    @Test
    fun `si ningun sendero coincide devuelve una lista vacia`() {
        assertTrue(filtrarSenderosPorDificultad(listOf(facil), setOf(Dificultad.ALTA)).isEmpty())
    }
}
