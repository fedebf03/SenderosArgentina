package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PuntoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

class CalculadoraCorredorTest {

    private val gradosPorMil = 1000.0 / 111195.0

    private fun punto(latitud: Double, longitud: Double) = PuntoRecorrido(latitud = latitud, longitud = longitud)

    @Test
    fun `sin puntos no hay rectangulos`() {
        assertTrue(CalculadoraCorredor.rectangulos(emptyList(), 1000.0).isEmpty())
    }

    @Test
    fun `un punto genera un rectangulo alrededor del punto`() {
        val rectangulos = CalculadoraCorredor.rectangulos(listOf(punto(0.0, 0.0)), 1000.0)

        assertEquals(1, rectangulos.size)
        assertEquals(-gradosPorMil, rectangulos[0].sur, 1e-9)
        assertEquals(gradosPorMil, rectangulos[0].norte, 1e-9)
        assertEquals(-gradosPorMil, rectangulos[0].oeste, 1e-9)
        assertEquals(gradosPorMil, rectangulos[0].este, 1e-9)
    }

    @Test
    fun `dos puntos en el ecuador generan un rectangulo ampliado`() {
        val rectangulos = CalculadoraCorredor.rectangulos(listOf(punto(0.0, 0.0), punto(0.0, 0.01)), 1000.0)

        assertEquals(1, rectangulos.size)
        assertEquals(-gradosPorMil, rectangulos[0].sur, 1e-9)
        assertEquals(gradosPorMil, rectangulos[0].norte, 1e-9)
        assertEquals(-gradosPorMil, rectangulos[0].oeste, 1e-9)
        assertEquals(0.01 + gradosPorMil, rectangulos[0].este, 1e-9)
    }

    @Test
    fun `tres puntos generan un rectangulo por tramo`() {
        val rectangulos = CalculadoraCorredor.rectangulos(
            listOf(punto(0.0, 0.0), punto(0.0, 0.01), punto(0.01, 0.01)),
            1000.0
        )

        assertEquals(2, rectangulos.size)
        assertEquals(0.01 + gradosPorMil, rectangulos[1].norte, 1e-9)
    }

    @Test
    fun `a sesenta grados de latitud la longitud se duplica`() {
        val rectangulos = CalculadoraCorredor.rectangulos(listOf(punto(60.0, 10.0), punto(60.0, 10.01)), 1000.0)

        val ampliacionLongitud = gradosPorMil / cos(Math.toRadians(60.0))
        assertEquals(10.0 - ampliacionLongitud, rectangulos[0].oeste, 1e-9)
        assertEquals(60.0 + gradosPorMil, rectangulos[0].norte, 1e-9)
    }

    @Test
    fun `los puntos invalidos se ignoran`() {
        val rectangulos = CalculadoraCorredor.rectangulos(
            listOf(punto(0.0, 0.0), punto(Double.NaN, 5.0), punto(200.0, 0.0), punto(0.0, 0.01)),
            1000.0
        )

        assertEquals(1, rectangulos.size)
        assertEquals(0.01 + gradosPorMil, rectangulos[0].este, 1e-9)
    }

    // Un tramo largo se subdivide para no cubrir un área enorme con un solo rectángulo
    @Test
    fun `un tramo diagonal de veinte kilometros se subdivide en rectangulos chicos`() {
        val rectangulos = CalculadoraCorredor.rectangulos(listOf(punto(0.0, 0.0), punto(0.127, 0.127)), 1000.0)

        assertTrue(rectangulos.size >= 10)
        assertTrue(rectangulos.all { rectangulo -> rectangulo.norte - rectangulo.sur < 0.04 })
        assertTrue(rectangulos.all { rectangulo -> rectangulo.este - rectangulo.oeste < 0.04 })
        assertEquals(-gradosPorMil, rectangulos.first().sur, 1e-9)
        assertEquals(0.127 + gradosPorMil, rectangulos.last().norte, 1e-9)
    }

    @Test
    fun `los limites quedan dentro del rango geografico`() {
        val rectangulo = CalculadoraCorredor.rectangulos(listOf(punto(89.999, 179.999)), 100000.0)[0]

        assertTrue(rectangulo.norte <= 90.0)
        assertTrue(rectangulo.este <= 180.0)
    }

    @Test
    fun `la firma es determinista`() {
        val trazado = listOf(punto(-40.5, -71.3), punto(-40.6, -71.4))

        assertEquals(CalculadoraCorredor.firmaTrazado(trazado), CalculadoraCorredor.firmaTrazado(trazado.toList()))
    }

    @Test
    fun `la firma cambia cuando cambia el trazado`() {
        val base = listOf(punto(-40.5, -71.3), punto(-40.6, -71.4))

        val firmaBase = CalculadoraCorredor.firmaTrazado(base)
        assertNotEquals(firmaBase, CalculadoraCorredor.firmaTrazado(listOf(punto(-40.5, -71.3), punto(-40.6, -71.41))))
        assertNotEquals(firmaBase, CalculadoraCorredor.firmaTrazado(base.reversed()))
        assertNotEquals(firmaBase, CalculadoraCorredor.firmaTrazado(base + punto(-40.7, -71.5)))
    }

    @Test
    fun `la firma ignora diferencias menores a seis decimales`() {
        val uno = listOf(punto(-40.5000001, -71.3000001))
        val otro = listOf(punto(-40.5000002, -71.3000002))

        assertEquals(CalculadoraCorredor.firmaTrazado(uno), CalculadoraCorredor.firmaTrazado(otro))
    }

    @Test
    fun `la firma ignora el nombre del punto`() {
        val sinNombre = listOf(punto(-40.5, -71.3))
        val conNombre = listOf(PuntoRecorrido(-40.5, -71.3, nombre = "Mirador"))

        assertEquals(CalculadoraCorredor.firmaTrazado(sinNombre), CalculadoraCorredor.firmaTrazado(conNombre))
    }
}
