package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraRecorridoTest {

    private val origen = PuntoRecorrido(latitud = 0.0, longitud = 0.0)
    private val unGradoAlEste = PuntoRecorrido(latitud = 0.0, longitud = 1.0)
    private val unGradoAlNorteYAlEste = PuntoRecorrido(latitud = 1.0, longitud = 1.0)

    private val kilometrosPorGradoEcuatorial = 111.195

    private val kilometrosDiagonal = 157.25

    @Test
    fun `distancia entre dos puntos conocidos coincide con Haversine`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste),
            TipoRecorrido.LINEAL
        )

        assertEquals(kilometrosPorGradoEcuatorial, distancia, 0.05)
    }

    @Test
    fun `lineal suma los tramos consecutivos sin cierre`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste),
            TipoRecorrido.LINEAL
        )

        assertEquals(kilometrosPorGradoEcuatorial * 2, distancia, 0.1)
    }

    @Test
    fun `circular suma el tramo de cierre del ultimo al primero`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste),
            TipoRecorrido.CIRCULAR
        )

        assertEquals(kilometrosPorGradoEcuatorial * 2 + kilometrosDiagonal, distancia, 0.5)
    }

    @Test
    fun `ida y vuelta duplica la distancia lineal`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste),
            TipoRecorrido.IDA_Y_VUELTA
        )

        assertEquals(kilometrosPorGradoEcuatorial * 4, distancia, 0.2)
    }

    @Test
    fun `con menos de dos puntos la distancia es cero`() {
        for (tipoRecorrido in TipoRecorrido.entries) {
            assertEquals(0.0, CalculadoraRecorrido.distanciaKilometros(emptyList(), tipoRecorrido), 0.0)
            assertEquals(0.0, CalculadoraRecorrido.distanciaKilometros(listOf(origen), tipoRecorrido), 0.0)
        }
    }

    @Test
    fun `invertir sentido devuelve los puntos en orden contrario`() {
        val puntos = listOf(
            PuntoRecorrido(1.0, 2.0, TipoPunto.REFUGIO, "Refugio"),
            PuntoRecorrido(3.0, 4.0, TipoPunto.PASO, ""),
            PuntoRecorrido(5.0, 6.0, TipoPunto.MIRADOR, "Cumbre")
        )

        val invertidos = CalculadoraRecorrido.invertirSentido(puntos)

        assertEquals(puntos.reversed(), invertidos)
    }

    @Test
    fun `invertir dos veces devuelve el original y la lista vacia sigue vacia`() {
        val puntos = listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste)

        val dobleInversion = CalculadoraRecorrido.invertirSentido(CalculadoraRecorrido.invertirSentido(puntos))

        assertEquals(puntos, dobleInversion)
        assertEquals(emptyList<PuntoRecorrido>(), CalculadoraRecorrido.invertirSentido(emptyList()))
    }
}
