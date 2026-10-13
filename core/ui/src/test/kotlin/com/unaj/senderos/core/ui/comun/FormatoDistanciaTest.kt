package com.unaj.senderos.presentation.componentes

import com.unaj.senderos.presentation.comun.formatearDistanciaAlUsuario
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoDistanciaTest {

    @Test
    fun `menos de un kilometro se muestra en metros`() {
        assertEquals("a 850 m de vos", formatearDistanciaAlUsuario(0.85))
    }

    @Test
    fun `hasta diez kilometros se muestra un decimal`() {
        assertEquals("a 1,5 km de vos", formatearDistanciaAlUsuario(1.5))
    }

    @Test
    fun `desde diez kilometros se muestra un entero`() {
        assertEquals("a 42 km de vos", formatearDistanciaAlUsuario(42.4))
    }

    // Verificar que 9,96 se redondea a 10 km y no a "10,0 km"
    @Test
    fun `el borde de diez kilometros usa el valor redondeado`() {
        assertEquals("a 10 km de vos", formatearDistanciaAlUsuario(9.96))
    }

    @Test
    fun `el borde de un kilometro no muestra mil metros`() {
        assertEquals("a 1,0 km de vos", formatearDistanciaAlUsuario(0.9996))
    }
}
