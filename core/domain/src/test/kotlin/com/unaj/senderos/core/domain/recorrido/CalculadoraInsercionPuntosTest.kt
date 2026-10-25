package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.DISTANCIA_MAXIMA_RECOMENDADA_METROS
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

class CalculadoraInsercionPuntosTest {

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

    private fun nombres(puntos: List<PuntoRecorrido>): List<String> {
        return puntos.map { punto -> punto.nombre }
    }

    @Test
    fun `un punto cerca del segundo tramo lineal se inserta entre B y C`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(0.006, 0.0001, "nuevo")))

        assertEquals(listOf("A", "B", "nuevo", "C"), nombres(resultado))
    }

    @Test
    fun `un punto cerca del primer tramo lineal se inserta entre A y B`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(0.001, 0.0001, "nuevo")))

        assertEquals(listOf("A", "nuevo", "B", "C"), nombres(resultado))
    }

    @Test
    fun `varios puntos en el mismo tramo se ordenan a lo largo del tramo`() {
        val capturadoPrimero = paso(0.007, 0.0001, "lejos")
        val capturadoDespues = paso(0.005, 0.0001, "cerca")

        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(capturadoPrimero, capturadoDespues))

        assertEquals(listOf("A", "B", "cerca", "lejos", "C"), nombres(resultado))
    }

    @Test
    fun `puntos de tramos distintos se reparten en cada tramo`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.007, 0.0001, "dos"), paso(0.001, 0.0001, "uno"))
        )

        assertEquals(listOf("A", "uno", "B", "dos", "C"), nombres(resultado))
    }

    @Test
    fun `en un circular un punto cerca del cierre se agrega al final`() {
        val resultado = CalculadoraInsercionPuntos.insertar(cuadrado, TipoRecorrido.CIRCULAR, listOf(paso(0.005, -0.0001, "cierre")))

        assertEquals(listOf("P0", "P1", "P2", "P3", "cierre"), nombres(resultado))
    }

    @Test
    fun `en un circular varios puntos del cierre se ordenan a lo largo del cierre`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            cuadrado,
            TipoRecorrido.CIRCULAR,
            listOf(paso(0.002, -0.0001, "cerca de P0"), paso(0.008, -0.0001, "cerca de P3"))
        )

        assertEquals(listOf("P0", "P1", "P2", "P3", "cerca de P3", "cerca de P0"), nombres(resultado))
    }

    @Test
    fun `en un circular un punto cerca de un tramo intermedio se inserta en el`() {
        val resultado = CalculadoraInsercionPuntos.insertar(cuadrado, TipoRecorrido.CIRCULAR, listOf(paso(0.005, 0.0101, "este")))

        assertEquals(listOf("P0", "P1", "este", "P2", "P3"), nombres(resultado))
    }

    @Test
    fun `en un lineal no existe tramo de cierre`() {
        val resultado = CalculadoraInsercionPuntos.insertar(cuadrado, TipoRecorrido.LINEAL, listOf(paso(0.002, 0.0001, "oeste")))

        assertEquals(listOf("P0", "oeste", "P1", "P2", "P3"), nombres(resultado))
    }

    @Test
    fun `en ida y vuelta se inserta en el tramo de ida mas cercano`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.IDA_Y_VUELTA, listOf(paso(0.007, 0.0001, "nuevo")))

        assertEquals(listOf("A", "B", "nuevo", "C"), nombres(resultado))
    }

    @Test
    fun `un punto mas alla del final lineal se inserta como anteultimo`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(0.012, 0.0, "pasado")))

        assertEquals(listOf("A", "B", "pasado", "C"), nombres(resultado))
    }

    @Test
    fun `un punto antes del inicio lineal se inserta como segundo vertice`() {
        val resultado = CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, listOf(paso(-0.004, 0.0, "previo")))

        assertEquals(listOf("A", "previo", "B", "C"), nombres(resultado))
    }

    @Test
    fun `en ida y vuelta los extremos tampoco cambian`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.IDA_Y_VUELTA,
            listOf(paso(-0.004, 0.0, "previo"), paso(0.012, 0.0, "pasado"))
        )

        assertEquals(listOf("A", "previo", "B", "pasado", "C"), nombres(resultado))
    }

    @Test
    fun `varios puntos antes del inicio se ordenan por distancia al inicio`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(-0.004, 0.0, "lejos"), paso(-0.002, 0.0, "cerca"))
        )

        assertEquals(listOf("A", "cerca", "lejos", "B", "C"), nombres(resultado))
    }

    @Test
    fun `varios puntos despues del final se ordenan acercandose al final`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.010, 0.0, "cerca"), paso(0.012, 0.0, "lejos"))
        )

        assertEquals(listOf("A", "B", "lejos", "cerca", "C"), nombres(resultado))
    }

    @Test
    fun `con dos vertices los extremos quedan entre ellos`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            listOf(paso(0.0, nombre = "A"), paso(0.004, nombre = "B")),
            TipoRecorrido.LINEAL,
            listOf(paso(0.006, 0.0, "pasado"), paso(-0.002, 0.0, "previo"))
        )

        assertEquals(listOf("A", "previo", "pasado", "B"), nombres(resultado))
    }

    @Test
    fun `un tramo degenerado al inicio no manda el punto antes del inicio`() {
        val conRepetido = listOf(paso(0.0, nombre = "A"), paso(0.0, nombre = "A repetido"), paso(0.008, nombre = "C"))

        val resultado = CalculadoraInsercionPuntos.insertar(conRepetido, TipoRecorrido.LINEAL, listOf(paso(-0.002, 0.0, "previo")))

        assertEquals("A", resultado.first().nombre)
        assertEquals(4, resultado.size)
    }

    @Test
    fun `sin recorrido los puntos quedan en orden de captura`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            emptyList(),
            TipoRecorrido.LINEAL,
            listOf(paso(0.002, nombre = "primero"), paso(0.001, nombre = "segundo"))
        )

        assertEquals(listOf("primero", "segundo"), nombres(resultado))
    }

    @Test
    fun `con un solo punto del recorrido los nuevos se agregan al final`() {
        val resultado = CalculadoraInsercionPuntos.insertar(
            listOf(paso(0.0, nombre = "A")),
            TipoRecorrido.LINEAL,
            listOf(paso(0.002, nombre = "x"), paso(0.001, nombre = "y"))
        )

        assertEquals(listOf("A", "x", "y"), nombres(resultado))
    }

    @Test
    fun `sin puntos nuevos el recorrido no cambia`() {
        assertEquals(tramoSur, CalculadoraInsercionPuntos.insertar(tramoSur, TipoRecorrido.LINEAL, emptyList()))
    }

    @Test
    fun `la distancia maxima recomendada es de cien metros`() {
        assertEquals(100.0, DISTANCIA_MAXIMA_RECOMENDADA_METROS, 0.0)
    }

    @Test
    fun `la distancia al trazado de un punto cercano es de unos ocho metros`() {
        val distancias = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.006, 0.0001))
        )

        assertEquals(8.5, distancias.single() ?: -1.0, 1.5)
    }

    @Test
    fun `un punto lejano supera la distancia recomendada`() {
        val distancia = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(0.006, 0.01))
        ).single() ?: -1.0

        assertTrue(distancia > DISTANCIA_MAXIMA_RECOMENDADA_METROS)
    }

    @Test
    fun `la distancia de un punto antes del inicio se mide al primer vertice`() {
        val distancia = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            tramoSur,
            TipoRecorrido.LINEAL,
            listOf(paso(-0.002, 0.0))
        ).single() ?: -1.0

        assertEquals(222.4, distancia, 3.0)
    }

    @Test
    fun `en un circular la distancia considera el tramo de cierre`() {
        val circular = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(cuadrado, TipoRecorrido.CIRCULAR, listOf(paso(0.005, -0.0001))).single() ?: -1.0
        val lineal = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(cuadrado, TipoRecorrido.LINEAL, listOf(paso(0.005, -0.0001))).single() ?: -1.0

        assertTrue(circular < 15.0)
        assertTrue(lineal > 100.0)
    }

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
