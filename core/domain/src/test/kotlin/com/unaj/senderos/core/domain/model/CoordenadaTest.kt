package com.unaj.senderos.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordenadaTest {

    // Verificar que una coordenada dentro de rango es válida, incluidos los límites
    @Test
    fun `una coordenada en rango es valida`() {
        assertTrue(Coordenada(-39.95, -71.07).tieneCoordenadasValidas())
        assertTrue(Coordenada(90.0, 180.0).tieneCoordenadasValidas())
        assertTrue(Coordenada(-90.0, -180.0).tieneCoordenadasValidas())
    }

    // Verificar que una coordenada fuera de rango es inválida
    @Test
    fun `una coordenada fuera de rango es invalida`() {
        assertFalse(Coordenada(90.1, 0.0).tieneCoordenadasValidas())
        assertFalse(Coordenada(0.0, -180.1).tieneCoordenadasValidas())
    }

    // Verificar que valores no finitos son inválidos
    @Test
    fun `una coordenada no finita es invalida`() {
        assertFalse(Coordenada(Double.NaN, 0.0).tieneCoordenadasValidas())
        assertFalse(Coordenada(0.0, Double.POSITIVE_INFINITY).tieneCoordenadasValidas())
    }
}
