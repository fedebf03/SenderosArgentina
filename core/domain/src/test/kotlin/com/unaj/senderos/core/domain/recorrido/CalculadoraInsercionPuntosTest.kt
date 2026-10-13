package com.unaj.senderos.domain.recorrido

import com.unaj.senderos.domain.model.DISTANCIA_MAXIMA_RECOMENDADA_METROS
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

class CalculadoraInsercionPuntosTest {

    // Construir un punto de paso desplazado en grados desde el origen de pruebas
    private fun paso(gradosAlSur: Double, gradosAlEste: Double = 0.0, nombre: String = ""): PuntoRecorrido {
        return PuntoRecorrido(LATITUD_BASE - gradosAlSur, LONGITUD_BASE + gradosAlEste, TipoPunto.PASO, nombre)
    }

    private val tramoSur = listOf(paso(0.0, nombre = "A"), paso(0.004, nombre = "B"), paso(0.008, nombre = "C"))

    private val cuadrado = listOf(
        paso(0.0, 0.0, "P0"),
        paso(0.0, 0.01, "P1"),
        paso(0.01, 0.01, "P2"),
        paso(0.01, 0.0, "P3")
    )

    // Obtener los nombres del recorrido resultante para comparar el orden
    private fun nombres(puntos: List<PuntoRecorrido>): List<String> {
        return puntos.map { punto -> punto.nombre }
    }

    // Verificar que un punto cerca del segundo tramo de un recorrido lineal se inserta entre B y C
    @Test
    fun `un punto cerca del segundo tramo lineal se inserta entre B y C`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(0.006, 0.0001, "nuevo")))

        assertEquals(listOf("A", "B", "nuevo", "C"), nombres(resultado))
    }

    // Verificar que un punto cerca del primer tramo se inserta entre A y B
    @Test
    fun `un punto cerca del primer tramo lineal se inserta entre A y B`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(0.001, 0.0001, "nuevo")))

        assertEquals(listOf("A", "nuevo", "B", "C"), nombres(resultado))
    }

    // Verificar que varios puntos del mismo tramo quedan ordenados a lo largo del tramo y no por captura
    @Test
    fun `varios puntos en el mismo tramo se ordenan a lo largo del tramo`() {
        val capturadoPrimero = paso(0.007, 0.0001, "lejos")
        val capturadoDespues = paso(0.005, 0.0001, "cerca")

        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(capturadoPrimero, capturadoDespues))

        assertEquals(listOf("A", "B", "cerca", "lejos", "C"), nombres(resultado))
    }

    // Verificar que puntos de tramos distintos se reparten cada uno en el suyo
    @Test
    fun `puntos de tramos distintos se reparten en cada tramo`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.007, 0.0001, "dos"), paso(0.001, 0.0001, "uno"))
        )

        assertEquals(listOf("A", "uno", "B", "dos", "C"), nombres(resultado))
    }

    // Verificar que en un circular un punto cerca del tramo de cierre se agrega al final
    @Test
    fun `en un circular un punto cerca del cierre se agrega al final`() {
        val resultado = CalculadoraInsercionPuntos.insertar(cuadrado, TipoRecorrido.CIRCULAR, listOf(paso(0.005, -0.0001, "cierre")))

        assertEquals(listOf("P0", "P1", "P2", "P3", "cierre"), nombres(resultado))
    }

    // Verificar que en un circular varios puntos del cierre se ordenan de P3 hacia P0
    @Test
    fun `en un circular varios puntos del cierre se ordenan a lo largo del cierre`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            cuadrado,
            TipoRecorrido.CIRCULAR,
            listOf(paso(0.002, -0.0001, "cerca de P0"), paso(0.008, -0.0001, "cerca de P3"))
        )

        assertEquals(listOf("P0", "P1", "P2", "P3", "cerca de P3", "cerca de P0"), nombres(resultado))
    }

    // Verificar que en un circular un punto cerca de un tramo intermedio se inserta en él
    @Test
    fun `en un circular un punto cerca de un tramo intermedio se inserta en el`() {
        val resultado = CalculadoraInsercionPuntos.insertar(cuadrado, TipoRecorrido.CIRCULAR, listOf(paso(0.005, 0.0101, "este")))

        assertEquals(listOf("P0", "P1", "este", "P2", "P3"), nombres(resultado))
    }

    // Verificar que en un lineal el lado de cierre no existe y el punto va al tramo real más cercano
    @Test
    fun `en un lineal no existe tramo de cierre`() {
        val resultado = CalculadoraInsercionPuntos.insertar(cuadrado, TipoRecorrido.LINEAL, listOf(paso(0.002, 0.0001, "oeste")))

        assertEquals(listOf("P0", "oeste", "P1", "P2", "P3"), nombres(resultado))
    }

    // Verificar que en ida y vuelta se usa el recorrido de ida sin tramo de cierre
    @Test
    fun `en ida y vuelta se inserta en el tramo de ida mas cercano`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.IDA_Y_VUELTA, listOf(paso(0.007, 0.0001, "nuevo")))

        assertEquals(listOf("A", "B", "nuevo", "C"), nombres(resultado))
    }

    // Verificar que un punto más allá del final de un lineal se inserta como anteúltimo y el final no cambia
    @Test
    fun `un punto mas alla del final lineal se inserta como anteultimo`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(0.012, 0.0, "pasado")))

        assertEquals(listOf("A", "B", "pasado", "C"), nombres(resultado))
    }

    // Verificar que un punto antes del inicio de un lineal se inserta como segundo vértice y el inicio no cambia
    @Test
    fun `un punto antes del inicio lineal se inserta como segundo vertice`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(-0.004, 0.0, "previo")))

        assertEquals(listOf("A", "previo", "B", "C"), nombres(resultado))
    }

    // Verificar que en ida y vuelta tampoco cambian el inicio ni el final
    @Test
    fun `en ida y vuelta los extremos tampoco cambian`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.IDA_Y_VUELTA,
            listOf(paso(-0.004, 0.0, "previo"), paso(0.012, 0.0, "pasado"))
        )

        assertEquals(listOf("A", "previo", "B", "pasado", "C"), nombres(resultado))
    }

    // Verificar que varios puntos antes del inicio se ordenan por distancia creciente al inicio
    @Test
    fun `varios puntos antes del inicio se ordenan por distancia al inicio`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(-0.004, 0.0, "lejos"), paso(-0.002, 0.0, "cerca"))
        )

        assertEquals(listOf("A", "cerca", "lejos", "B", "C"), nombres(resultado))
    }

    // Verificar que varios puntos después del final se ordenan acercándose al final
    @Test
    fun `varios puntos despues del final se ordenan acercandose al final`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.010, 0.0, "cerca"), paso(0.012, 0.0, "lejos"))
        )

        assertEquals(listOf("A", "B", "lejos", "cerca", "C"), nombres(resultado))
    }

    // Verificar que con dos vértices los puntos de ambos extremos quedan entre ellos en orden de recorrido
    @Test
    fun `con dos vertices los extremos quedan entre ellos`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            listOf(paso(0.0, nombre = "A"), paso(0.004, nombre = "B")),
            TipoRecorrido.LINEAL,
            listOf(paso(0.006, 0.0, "pasado"), paso(-0.002, 0.0, "previo"))
        )

        assertEquals(listOf("A", "previo", "pasado", "B"), nombres(resultado))
    }

    // Verificar que un tramo degenerado al inicio no hace que el punto vaya antes del primer vértice
    @Test
    fun `un tramo degenerado al inicio no manda el punto antes del inicio`() {
        val conRepetido = listOf(paso(0.0, nombre = "A"), paso(0.0, nombre = "A repetido"), paso(0.008, nombre = "C"))

        val resultado = CalculadoraInsercionPuntos.insertar(conRepetido, TipoRecorrido.LINEAL, listOf(paso(-0.002, 0.0, "previo")))

        assertEquals("A", resultado.first().nombre)
        assertEquals(4, resultado.size)
    }

    // Verificar que sin recorrido los puntos se agregan en orden de captura
    @Test
    fun `sin recorrido los puntos quedan en orden de captura`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            emptyList(),
            TipoRecorrido.LINEAL,
            listOf(paso(0.002, nombre = "primero"), paso(0.001, nombre = "segundo"))
        )

        assertEquals(listOf("primero", "segundo"), nombres(resultado))
    }

    // Verificar que con un solo punto de recorrido (sin tramos) los nuevos se agregan al final en orden de captura
    @Test
    fun `con un solo punto del recorrido los nuevos se agregan al final`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            listOf(paso(0.0, nombre = "A")),
            TipoRecorrido.LINEAL,
            listOf(paso(0.002, nombre = "x"), paso(0.001, nombre = "y"))
        )

        assertEquals(listOf("A", "x", "y"), nombres(resultado))
    }

    // Verificar que sin puntos nuevos el recorrido queda igual
    @Test
    fun `sin puntos nuevos el recorrido no cambia`() {
        assertEquals(tramoSur, CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, emptyList()))
    }

    // Verificar que la distancia máxima recomendada es de 100 metros
    @Test
    fun `la distancia maxima recomendada es de cien metros`() {
        assertEquals(100.0, DISTANCIA_MAXIMA_RECOMENDADA_METROS, 0.0)
    }

    // Verificar que la distancia al trazado de un punto cercano ronda los 8,5 metros
    @Test
    fun `la distancia al trazado de un punto cercano es de unos ocho metros`() {
        val distancias = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.006, 0.0001))
        )

        assertEquals(8.5, distancias.single() ?: -1.0, 1.5)
    }

    // Verificar que un punto a 0,01 grados al este supera la distancia recomendada
    @Test
    fun `un punto lejano supera la distancia recomendada`() {
        val distancia = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.006, 0.01))
        ).single() ?: -1.0

        assertTrue(distancia > DISTANCIA_MAXIMA_RECOMENDADA_METROS)
    }

    // Verificar que la distancia de un punto antes del inicio se mide al primer vértice
    @Test
    fun `la distancia de un punto antes del inicio se mide al primer vertice`() {
        val distancia = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(-0.002, 0.0))
        ).single() ?: -1.0

        assertEquals(222.4, distancia, 3.0)
    }

    // Verificar que en un circular se considera el tramo de cierre
    @Test
    fun `en un circular la distancia considera el tramo de cierre`() {
        val circular = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(cuadrado, TipoRecorrido.CIRCULAR, listOf(paso(0.005, -0.0001))).single() ?: -1.0
        val lineal = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(cuadrado, TipoRecorrido.LINEAL, listOf(paso(0.005, -0.0001))).single() ?: -1.0

        assertTrue(circular < 15.0)
        assertTrue(lineal > 100.0)
    }

    // Verificar que sin recorrido la distancia es desconocida y con un solo vértice se mide a él
    @Test
    fun `sin recorrido la distancia es desconocida y con un vertice se mide a el`() {
        assertNull(CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(emptyList(), TipoRecorrido.LINEAL, listOf(paso(0.0))).single())
        val distancia = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            listOf(paso(0.0, nombre = "A")),
            TipoRecorrido.LINEAL,
            listOf(paso(0.002, 0.0))
        ).single() ?: -1.0
        assertEquals(222.4, distancia, 3.0)
    }

    // Verificar que se devuelve una distancia por cada punto en el orden recibido
    @Test
    fun `se devuelve una distancia por cada punto en el orden recibido`() {
        val distancias = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.006, 0.01), paso(0.006, 0.0001))
        )

        assertEquals(2, distancias.size)
        assertTrue((distancias[0] ?: 0.0) > (distancias[1] ?: 0.0))
    }
}
