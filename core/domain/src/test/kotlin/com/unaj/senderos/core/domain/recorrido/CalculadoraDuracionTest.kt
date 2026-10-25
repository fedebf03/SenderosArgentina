package com.unaj.senderos.core.domain.recorrido

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Regla de Naismith: 4 km por hora en llano más una hora cada 600 m de subida, redondeado a 5 minutos
class CalculadoraDuracionTest {

    @Test
    fun `ocho kilometros en llano son dos horas`() {
        assertEquals(120, CalculadoraDuracion.estimarMinutos(distanciaKilometros = 8.0, desnivelSubidaMetros = 0))
    }

    @Test
    fun `seiscientos metros de subida suman una hora`() {
        assertEquals(210, CalculadoraDuracion.estimarMinutos(distanciaKilometros = 10.0, desnivelSubidaMetros = 600))
    }

    @Test
    fun `el resultado se redondea a cinco minutos`() {
        // 3,3 km son 49,5 minutos
        assertEquals(50, CalculadoraDuracion.estimarMinutos(distanciaKilometros = 3.3, desnivelSubidaMetros = 0))
    }

    @Test
    fun `un desnivel negativo no resta tiempo`() {
        assertEquals(120, CalculadoraDuracion.estimarMinutos(distanciaKilometros = 8.0, desnivelSubidaMetros = -300))
    }

    @Test
    fun `una distancia absurda no da estimacion`() {
        assertNull(CalculadoraDuracion.estimarMinutos(distanciaKilometros = 1.5e8, desnivelSubidaMetros = 0))
    }

    @Test
    fun `sin distancia no hay estimacion`() {
        assertNull(CalculadoraDuracion.estimarMinutos(distanciaKilometros = 0.0, desnivelSubidaMetros = 500))
        assertNull(CalculadoraDuracion.estimarMinutos(distanciaKilometros = Double.NaN, desnivelSubidaMetros = 0))
    }
}
