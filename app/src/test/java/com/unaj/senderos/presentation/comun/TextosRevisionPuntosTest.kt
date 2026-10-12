package com.unaj.senderos.presentation.comun

import org.junit.Assert.assertEquals
import org.junit.Test

class TextosRevisionPuntosTest {

    // Verificar el singular y el plural del contador del panel
    @Test
    fun `el contador de puntos capturados usa singular y plural`() {
        assertEquals("0 puntos capturados", describirCantidadPuntosCapturados(0))
        assertEquals("1 punto capturado", describirCantidadPuntosCapturados(1))
        assertEquals("3 puntos capturados", describirCantidadPuntosCapturados(3))
    }

    // Verificar el singular y el plural de la tarjeta del detalle
    @Test
    fun `la tarjeta de revision usa singular y plural`() {
        assertEquals("1 punto por revisar", describirCantidadPuntosPorRevisar(1))
        assertEquals("2 puntos por revisar", describirCantidadPuntosPorRevisar(2))
    }

    // Verificar la distancia al trazado en metros y en kilómetros
    @Test
    fun `la distancia al trazado se describe en metros o kilometros`() {
        assertEquals("a 35 m del trazado", describirDistanciaAlTrazado(34.6))
        assertEquals("a 1,5 km del trazado", describirDistanciaAlTrazado(1_540.0))
    }

    // Verificar el aviso de posición antigua redondeado a segundos
    @Test
    fun `el aviso de posicion antigua indica los segundos`() {
        assertEquals(
            "Punto guardado con la posición de hace 12 s (señal débil)",
            describirPuntoGuardadoConPosicionAntigua(12_400L)
        )
    }
}
