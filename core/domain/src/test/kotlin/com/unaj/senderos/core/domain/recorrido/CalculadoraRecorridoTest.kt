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

    // Distancia esperada de un grado sobre el ecuador con radio terrestre de 6371 km
    private val kilometrosPorGradoEcuatorial = 111.195

    // Distancia esperada del tercer lado del triángulo formado por los tres puntos de prueba
    private val kilometrosDiagonal = 157.25

    // Probar que la distancia entre dos puntos conocidos coincide con Haversine y radio 6371 km
    @Test
    fun `distancia entre dos puntos conocidos coincide con Haversine`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste),
            TipoRecorrido.LINEAL
        )

        assertEquals(kilometrosPorGradoEcuatorial, distancia, 0.05)
    }

    // Probar que la distancia lineal suma los tramos consecutivos sin cerrar el recorrido
    @Test
    fun `lineal suma los tramos consecutivos sin cierre`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste),
            TipoRecorrido.LINEAL
        )

        assertEquals(kilometrosPorGradoEcuatorial * 2, distancia, 0.1)
    }

    // Probar que el recorrido circular suma además el tramo del último punto al primero
    @Test
    fun `circular suma el tramo de cierre del ultimo al primero`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste),
            TipoRecorrido.CIRCULAR
        )

        assertEquals(kilometrosPorGradoEcuatorial * 2 + kilometrosDiagonal, distancia, 0.5)
    }

    // Probar que el recorrido de ida y vuelta duplica la distancia lineal
    @Test
    fun `ida y vuelta duplica la distancia lineal`() {
        val distancia = CalculadoraRecorrido.distanciaKilometros(
            listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste),
            TipoRecorrido.IDA_Y_VUELTA
        )

        assertEquals(kilometrosPorGradoEcuatorial * 4, distancia, 0.2)
    }

    // Probar que con menos de dos puntos la distancia es cero para todos los tipos de recorrido
    @Test
    fun `con menos de dos puntos la distancia es cero`() {
        for (tipoRecorrido in TipoRecorrido.entries) {
            assertEquals(0.0, CalculadoraRecorrido.distanciaKilometros(emptyList(), tipoRecorrido), 0.0)
            assertEquals(0.0, CalculadoraRecorrido.distanciaKilometros(listOf(origen), tipoRecorrido), 0.0)
        }
    }

    // Probar que invertir el sentido devuelve los puntos en orden contrario conservando tipo y nombre
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

    // Probar que invertir dos veces devuelve el recorrido original y que una lista vacía sigue vacía
    @Test
    fun `invertir dos veces devuelve el original y la lista vacia sigue vacia`() {
        val puntos = listOf(origen, unGradoAlEste, unGradoAlNorteYAlEste)

        val dobleInversion = CalculadoraRecorrido.invertirSentido(CalculadoraRecorrido.invertirSentido(puntos))

        assertEquals(puntos, dobleInversion)
        assertEquals(emptyList<PuntoRecorrido>(), CalculadoraRecorrido.invertirSentido(emptyList()))
    }
}
