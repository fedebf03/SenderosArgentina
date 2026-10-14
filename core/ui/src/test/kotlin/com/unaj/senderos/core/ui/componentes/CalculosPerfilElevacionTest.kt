package com.unaj.senderos.core.ui.componentes

import com.unaj.senderos.core.domain.model.PuntoPerfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculosPerfilElevacionTest {

    private fun rampa(cantidad: Int, altitudInicial: Double, pasoAltitud: Double, pasoKilometros: Double = 1.0) =
        (0 until cantidad).map { indice ->
            PuntoPerfil(indice * pasoKilometros, altitudInicial + indice * pasoAltitud)
        }

    // Escala

    @Test
    fun `la escala cubre las alturas con marcas redondas dentro del rango`() {
        val puntos = listOf(PuntoPerfil(0.0, 1400.0), PuntoPerfil(5.0, 2000.0), PuntoPerfil(10.0, 1500.0))

        val escala = calcularEscalaPerfil(puntos)

        assertEquals(10.0, escala.kilometrosTotales, 0.0001)
        assertEquals(listOf(1400, 1600, 1800, 2000), escala.marcasAltitudMetros)
        assertTrue(escala.altitudMinimaEjeMetros <= 1400.0)
        assertTrue(escala.altitudMaximaEjeMetros >= 2000.0)
    }

    @Test
    fun `las marcas de altura son entre dos y cuatro y quedan dentro del eje`() {
        val casos = listOf(
            listOf(1403.0, 2011.0), listOf(0.0, 35.0), listOf(-105.0, 300.0),
            listOf(3200.0, 6900.0), listOf(12.0, 18.0), listOf(500.0, 502.0)
        )
        casos.forEach { (minima, maxima) ->
            val escala = calcularEscalaPerfil(listOf(PuntoPerfil(0.0, minima), PuntoPerfil(3.0, maxima)))

            assertTrue("marcas ${escala.marcasAltitudMetros}", escala.marcasAltitudMetros.size in 2..4)
            escala.marcasAltitudMetros.forEach { marca ->
                assertTrue(marca >= escala.altitudMinimaEjeMetros && marca <= escala.altitudMaximaEjeMetros)
            }
            assertTrue(escala.altitudMinimaEjeMetros <= minima && escala.altitudMaximaEjeMetros >= maxima)
        }
    }

    @Test
    fun `un perfil plano obtiene un rango mínimo para no dividir por cero`() {
        val escala = calcularEscalaPerfil(listOf(PuntoPerfil(0.0, 800.0), PuntoPerfil(2.0, 800.0)))

        assertTrue(escala.altitudMaximaEjeMetros - escala.altitudMinimaEjeMetros >= 10.0)
        assertTrue(escala.altitudMinimaEjeMetros < 800.0 && escala.altitudMaximaEjeMetros > 800.0)
        assertTrue(escala.marcasAltitudMetros.isNotEmpty())
    }

    @Test
    fun `sin puntos la escala es vacía y no falla`() {
        val escala = calcularEscalaPerfil(emptyList())

        assertEquals(0.0, escala.kilometrosTotales, 0.0)
        assertTrue(escala.marcasAltitudMetros.isEmpty())
    }

    @Test
    fun `las marcas de kilómetros son múltiplos redondos desde cero y a lo sumo cinco`() {
        assertEquals(listOf(0.0, 5.0, 10.0), calcularMarcasKilometros(12.0))
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0), calcularMarcasKilometros(4.0))
        assertEquals(listOf(0.0, 0.2, 0.4, 0.6, 0.8), calcularMarcasKilometros(0.8))
        listOf(0.3, 7.0, 21.5, 120.0, 570.0).forEach { total ->
            val marcas = calcularMarcasKilometros(total)
            assertTrue("total $total marcas $marcas", marcas.size in 2..5)
            assertTrue(marcas.last() <= total)
        }
    }

    @Test
    fun `sin largo la única marca de kilómetros es cero`() {
        assertEquals(listOf(0.0), calcularMarcasKilometros(0.0))
    }

    // Toque

    @Test
    fun `la posición del toque se convierte a kilómetros dentro del área`() {
        assertEquals(0.0, convertirPosicionAKilometros(100f, 100f, 500f, 8.0), 0.0001)
        assertEquals(4.0, convertirPosicionAKilometros(300f, 100f, 500f, 8.0), 0.0001)
        assertEquals(8.0, convertirPosicionAKilometros(500f, 100f, 500f, 8.0), 0.0001)
    }

    @Test
    fun `un toque fuera del área se limita a los extremos`() {
        assertEquals(0.0, convertirPosicionAKilometros(20f, 100f, 500f, 8.0), 0.0001)
        assertEquals(8.0, convertirPosicionAKilometros(900f, 100f, 500f, 8.0), 0.0001)
    }

    @Test
    fun `un área sin ancho devuelve cero`() {
        assertEquals(0.0, convertirPosicionAKilometros(50f, 100f, 100f, 8.0), 0.0)
    }

    @Test
    fun `el punto más cercano al toque es el de menor distancia en kilómetros`() {
        val puntos = listOf(PuntoPerfil(0.0, 10.0), PuntoPerfil(0.5, 20.0), PuntoPerfil(1.0, 30.0))

        assertEquals(1, buscarIndicePuntoMasCercano(puntos, 0.6))
        assertEquals(0, buscarIndicePuntoMasCercano(puntos, 0.2))
        assertEquals(2, buscarIndicePuntoMasCercano(puntos, 0.9))
    }

    @Test
    fun `el punto más cercano se limita a los extremos y es nulo sin puntos`() {
        val puntos = listOf(PuntoPerfil(0.0, 10.0), PuntoPerfil(0.5, 20.0), PuntoPerfil(1.0, 30.0))

        assertEquals(0, buscarIndicePuntoMasCercano(puntos, -3.0))
        assertEquals(2, buscarIndicePuntoMasCercano(puntos, 99.0))
        assertNull(buscarIndicePuntoMasCercano(emptyList(), 1.0))
    }

    @Test
    fun `la altitud se interpola entre las muestras vecinas`() {
        val puntos = listOf(PuntoPerfil(0.0, 100.0), PuntoPerfil(2.0, 200.0))

        assertEquals(150.0, interpolarAltitud(puntos, 1.0) ?: Double.NaN, 0.0001)
        assertEquals(100.0, interpolarAltitud(puntos, -1.0) ?: Double.NaN, 0.0001)
        assertEquals(200.0, interpolarAltitud(puntos, 5.0) ?: Double.NaN, 0.0001)
        assertNull(interpolarAltitud(emptyList(), 1.0))
    }

    // Kilómetro en el trazado

    @Test
    fun `el kilómetro en el trazado es el largo total menos lo que falta`() {
        assertEquals(6.0, calcularKilometroEnTrazado(10.0, 4.0), 0.0001)
    }

    @Test
    fun `el kilómetro en el trazado se limita entre cero y el largo`() {
        assertEquals(0.0, calcularKilometroEnTrazado(10.0, 12.0), 0.0)
        assertEquals(10.0, calcularKilometroEnTrazado(10.0, -1.0), 0.0)
    }
}
