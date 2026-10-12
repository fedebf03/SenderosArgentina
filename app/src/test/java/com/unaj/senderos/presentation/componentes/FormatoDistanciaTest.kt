package com.unaj.senderos.presentation.componentes

import com.unaj.senderos.presentation.comun.formatearDistanciaAlUsuario
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoDistanciaTest {

    // Verificar que menos de un kilómetro se expresa en metros
    @Test
    fun `menos de un kilometro se muestra en metros`() {
        assertEquals("a 850 m de vos", formatearDistanciaAlUsuario(0.85))
    }

    // Verificar que hasta diez kilómetros se muestra un decimal con coma
    @Test
    fun `hasta diez kilometros se muestra un decimal`() {
        assertEquals("a 1,5 km de vos", formatearDistanciaAlUsuario(1.5))
    }

    // Verificar que desde diez kilómetros se muestra un entero
    @Test
    fun `desde diez kilometros se muestra un entero`() {
        assertEquals("a 42 km de vos", formatearDistanciaAlUsuario(42.4))
    }

    // Verificar que 9,96 se redondea a 10 km y no a "10,0 km"
    @Test
    fun `el borde de diez kilometros usa el valor redondeado`() {
        assertEquals("a 10 km de vos", formatearDistanciaAlUsuario(9.96))
    }

    // Verificar que 999,6 metros pasa a kilómetros en lugar de mostrar 1000 m
    @Test
    fun `el borde de un kilometro no muestra mil metros`() {
        assertEquals("a 1,0 km de vos", formatearDistanciaAlUsuario(0.9996))
    }
}
