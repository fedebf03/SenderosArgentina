package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculadoraPerfilTest {

    private fun punto(latitud: Double, longitud: Double = -70.0) = PuntoRecorrido(latitud, longitud)

    // Dos puntos separados 0,01 grado de latitud, unos 1,11 km
    private val tramoRecto = listOf(punto(-40.0), punto(-40.01))

    private val triangulo = listOf(punto(-40.0), punto(-40.01), punto(-40.01, -69.99))

    // Calcular altitudes que crecen linealmente con la distancia desde inicio hasta fin
    private fun altitudesLineales(muestras: List<MuestraTrazado>, inicio: Double, fin: Double): List<Double> {
        val total = muestras.last().distanciaKilometros
        return muestras.map { muestra -> inicio + (fin - inicio) * muestra.distanciaKilometros / total }
    }

    private fun muestrasSinteticas(cantidad: Int, pasoKilometros: Double = 0.05): List<MuestraTrazado> {
        return List(cantidad) { indice ->
            MuestraTrazado(Coordenada(-40.0 - indice * 0.0004, -70.0), indice * pasoKilometros)
        }
    }

    @Test
    fun `el muestreo incluye inicio y fin y separa las muestras unos 50 metros`() {
        val muestras = CalculadoraPerfil.muestrearTrazado(tramoRecto, TipoRecorrido.LINEAL)

        assertEquals(0.0, muestras.first().distanciaKilometros, 1e-9)
        assertEquals(-40.0, muestras.first().coordenada.latitud, 1e-9)
        assertEquals(-40.01, muestras.last().coordenada.latitud, 1e-9)
        assertEquals(CalculadoraRecorrido.distanciaKilometros(tramoRecto, TipoRecorrido.LINEAL), muestras.last().distanciaKilometros, 1e-6)
        val pasos = muestras.zipWithNext { anterior, siguiente -> (siguiente.distanciaKilometros - anterior.distanciaKilometros) * 1000.0 }
        assertTrue(pasos.all { paso -> paso in 40.0..50.0001 })
        assertEquals(24, muestras.size)
    }

    @Test
    fun `un trazado de menos de dos puntos validos no tiene muestras`() {
        assertTrue(CalculadoraPerfil.muestrearTrazado(emptyList(), TipoRecorrido.LINEAL).isEmpty())
        assertTrue(CalculadoraPerfil.muestrearTrazado(listOf(punto(-40.0)), TipoRecorrido.LINEAL).isEmpty())
        assertTrue(CalculadoraPerfil.muestrearTrazado(listOf(punto(-40.0), punto(Double.NaN)), TipoRecorrido.LINEAL).isEmpty())
        assertTrue(CalculadoraPerfil.muestrearTrazado(listOf(punto(-40.0), punto(-40.0)), TipoRecorrido.LINEAL).isEmpty())
    }

    @Test
    fun `el muestreo respeta el tope de puntos aumentando el paso`() {
        val trazadoLargo = listOf(punto(-40.0), punto(-41.0))

        val muestras = CalculadoraPerfil.muestrearTrazado(trazadoLargo, TipoRecorrido.LINEAL)

        assertEquals(CalculadoraPerfil.MAXIMO_PUNTOS, muestras.size)
        assertEquals(CalculadoraRecorrido.distanciaKilometros(trazadoLargo, TipoRecorrido.LINEAL), muestras.last().distanciaKilometros, 1e-6)
        assertEquals(-41.0, muestras.last().coordenada.latitud, 1e-9)
    }

    @Test
    fun `un recorrido de ida y vuelta se muestrea con la vuelta`() {
        val muestras = CalculadoraPerfil.muestrearTrazado(tramoRecto, TipoRecorrido.IDA_Y_VUELTA)

        assertEquals(CalculadoraRecorrido.distanciaKilometros(tramoRecto, TipoRecorrido.IDA_Y_VUELTA), muestras.last().distanciaKilometros, 1e-6)
        assertEquals(-40.0, muestras.last().coordenada.latitud, 1e-9)
        assertTrue(muestras.any { muestra -> muestra.coordenada.latitud <= -40.009 })
    }

    @Test
    fun `un recorrido circular se muestrea cerrando el circuito`() {
        val muestras = CalculadoraPerfil.muestrearTrazado(triangulo, TipoRecorrido.CIRCULAR)

        assertEquals(CalculadoraRecorrido.distanciaKilometros(triangulo, TipoRecorrido.CIRCULAR), muestras.last().distanciaKilometros, 1e-6)
        assertEquals(-40.0, muestras.last().coordenada.latitud, 1e-9)
        assertEquals(-70.0, muestras.last().coordenada.longitud, 1e-9)
    }

    @Test
    fun `una subida pura suma desnivel positivo y ninguno negativo`() {
        val muestras = CalculadoraPerfil.muestrearTrazado(tramoRecto, TipoRecorrido.LINEAL)

        val perfil = CalculadoraPerfil.calcular(muestras, altitudesLineales(muestras, 1000.0, 1100.0), "firma")

        assertEquals(100.0, perfil.desnivelPositivoMetros.toDouble(), 1.0)
        assertEquals(0, perfil.desnivelNegativoMetros)
        assertEquals(1100, perfil.alturaMaximaMetros)
        assertEquals(1000, perfil.alturaMinimaMetros)
        assertEquals(100.0 / (muestras.last().distanciaKilometros * 1000.0) * 100.0, perfil.pendienteMaximaPorcentaje, 0.1)
        assertEquals(muestras.size, perfil.puntos.size)
        assertEquals("firma", perfil.firmaTrazado)
        assertEquals(0.0, perfil.puntos.first().distanciaKilometros, 1e-9)
        assertEquals(1000.0, perfil.puntos.first().altitudMetros, 1e-9)
    }

    @Test
    fun `una bajada pura suma desnivel negativo y ninguno positivo`() {
        val muestras = CalculadoraPerfil.muestrearTrazado(tramoRecto, TipoRecorrido.LINEAL)

        val perfil = CalculadoraPerfil.calcular(muestras, altitudesLineales(muestras, 1100.0, 1000.0), "firma")

        assertEquals(0, perfil.desnivelPositivoMetros)
        assertEquals(100.0, perfil.desnivelNegativoMetros.toDouble(), 1.0)
        assertEquals(1100, perfil.alturaMaximaMetros)
        assertEquals(1000, perfil.alturaMinimaMetros)
        assertTrue(perfil.pendienteMaximaPorcentaje > 0.0)
    }

    @Test
    fun `una ida y vuelta por el mismo camino tiene desnivel positivo y negativo parecidos`() {
        val muestras = CalculadoraPerfil.muestrearTrazado(tramoRecto, TipoRecorrido.IDA_Y_VUELTA)
        val total = muestras.last().distanciaKilometros
        val altitudes = muestras.map { muestra ->
            1000.0 + 100.0 * (1.0 - abs(2 * muestra.distanciaKilometros / total - 1.0))
        }

        val perfil = CalculadoraPerfil.calcular(muestras, altitudes, "firma")

        assertEquals(100.0, perfil.desnivelPositivoMetros.toDouble(), 8.0)
        assertEquals(100.0, perfil.desnivelNegativoMetros.toDouble(), 8.0)
        assertEquals(1100.0, perfil.alturaMaximaMetros.toDouble(), 3.0)
    }

    @Test
    fun `un circuito circular sube y baja lo mismo`() {
        val muestras = CalculadoraPerfil.muestrearTrazado(triangulo, TipoRecorrido.CIRCULAR)
        val total = muestras.last().distanciaKilometros
        val altitudes = muestras.map { muestra ->
            1000.0 + 200.0 * (1.0 - abs(2 * muestra.distanciaKilometros / total - 1.0))
        }

        val perfil = CalculadoraPerfil.calcular(muestras, altitudes, "firma")

        assertEquals(200.0, perfil.desnivelPositivoMetros.toDouble(), 10.0)
        assertEquals(perfil.desnivelPositivoMetros.toDouble(), perfil.desnivelNegativoMetros.toDouble(), 2.0)
    }

    @Test
    fun `la pendiente maxima de una rampa constante es la de la rampa`() {
        val muestras = muestrasSinteticas(21)
        val altitudes = List(21) { indice -> 1000.0 + 10.0 * indice }

        val perfil = CalculadoraPerfil.calcular(muestras, altitudes, "firma")

        assertEquals(20.0, perfil.pendienteMaximaPorcentaje, 0.01)
        assertEquals(200.0, perfil.desnivelPositivoMetros.toDouble(), 0.5)
    }

    @Test
    fun `un escalon aislado del modelo se suaviza y no se toma como pared`() {
        val muestras = muestrasSinteticas(21)
        val altitudes = List(21) { indice -> if (indice < 10) 1000.0 else 1100.0 }

        val perfil = CalculadoraPerfil.calcular(muestras, altitudes, "firma")

        assertTrue(perfil.pendienteMaximaPorcentaje in 20.0..60.0)
        assertEquals(100.0, perfil.desnivelPositivoMetros.toDouble(), 1.0)
    }

    @Test
    fun `el ruido alternado del modelo casi no suma desnivel`() {
        val muestras = muestrasSinteticas(41)
        val altitudes = List(41) { indice -> if (indice % 2 == 0) 1002.0 else 998.0 }
        val sumaSinSuavizar = 20 * 4

        val perfil = CalculadoraPerfil.calcular(muestras, altitudes, "firma")

        assertTrue(perfil.desnivelPositivoMetros < sumaSinSuavizar / 3)
        assertTrue(perfil.desnivelNegativoMetros < sumaSinSuavizar / 3)
        assertEquals(1002, perfil.alturaMaximaMetros)
        assertEquals(998, perfil.alturaMinimaMetros)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calcular con distinta cantidad de altitudes y muestras falla`() {
        CalculadoraPerfil.calcular(muestrasSinteticas(5), listOf(1.0, 2.0), "firma")
    }

    private fun perfilCon(positivo: Int, maxima: Int, minima: Int, puntos: List<PuntoPerfil> = emptyList()) = PerfilElevacion(
        puntos = puntos,
        desnivelPositivoMetros = positivo,
        desnivelNegativoMetros = 0,
        alturaMaximaMetros = maxima,
        alturaMinimaMetros = minima,
        pendienteMaximaPorcentaje = 1.0,
        firmaTrazado = "firma"
    )

    @Test
    fun `el autocompletado solo completa los valores que son exactamente cero`() {
        val sendero = crearSenderoDePrueba(desnivelMetros = 0, alturaMaximaMetros = 0, alturaMinimaMetros = -50)

        val resultado = CalculadoraPerfil.aplicarAlSendero(sendero, perfilCon(120, 1500, 1000))

        assertEquals(120, resultado.desnivelMetros)
        assertEquals(1500, resultado.alturaMaximaMetros)
        assertEquals(-50, resultado.alturaMinimaMetros)
    }

    @Test
    fun `un recalculo actualiza los campos que la app habia autocompletado y respeta los editados`() {
        val anterior = perfilCon(100, 1500, 1000)
        val sendero = crearSenderoDePrueba(
            desnivelMetros = 100,
            alturaMaximaMetros = 1500,
            alturaMinimaMetros = 900,
            perfilElevacion = anterior
        )

        val resultado = CalculadoraPerfil.aplicarAlSendero(sendero, perfilCon(200, 1700, 1000))

        assertEquals(200, resultado.desnivelMetros)
        assertEquals(1700, resultado.alturaMaximaMetros)
        assertEquals(900, resultado.alturaMinimaMetros)
    }

    private val perfilRampa = perfilCon(
        positivo = 100, maxima = 1100, minima = 1000,
        puntos = List(11) { indice -> PuntoPerfil(indice * 0.1, 1000.0 + indice * 10.0) }
    )

    @Test
    fun `la subida restante desde el inicio es el desnivel positivo y desde el final es cero`() {
        assertEquals(100.0, CalculadoraPerfil.calcularSubidaRestanteMetros(perfilRampa, 0.0).toDouble(), 1.0)
        assertEquals(100.0, CalculadoraPerfil.calcularSubidaRestanteMetros(perfilRampa, -5.0).toDouble(), 1.0)
        assertEquals(0, CalculadoraPerfil.calcularSubidaRestanteMetros(perfilRampa, 1.0))
        assertEquals(0, CalculadoraPerfil.calcularSubidaRestanteMetros(perfilRampa, 9.0))
    }

    @Test
    fun `la subida restante a mitad de camino es la mitad y una bajada no suma`() {
        assertEquals(50.0, CalculadoraPerfil.calcularSubidaRestanteMetros(perfilRampa, 0.5).toDouble(), 1.0)
        assertEquals(45.0, CalculadoraPerfil.calcularSubidaRestanteMetros(perfilRampa, 0.55).toDouble(), 1.0)
        val bajada = perfilCon(0, 1100, 1000, List(11) { indice -> PuntoPerfil(indice * 0.1, 1100.0 - indice * 10.0) })
        assertEquals(0, CalculadoraPerfil.calcularSubidaRestanteMetros(bajada, 0.3))
        assertEquals(0, CalculadoraPerfil.calcularSubidaRestanteMetros(perfilCon(0, 0, 0), 0.0))
    }

    @Test
    fun `la firma cambia al mover un punto o al cambiar el tipo de recorrido`() {
        val base = CalculadoraPerfil.firmaTrazado(tramoRecto, TipoRecorrido.LINEAL)
        val movido = CalculadoraPerfil.firmaTrazado(listOf(punto(-40.0), punto(-40.02)), TipoRecorrido.LINEAL)
        val deIdaYVuelta = CalculadoraPerfil.firmaTrazado(tramoRecto, TipoRecorrido.IDA_Y_VUELTA)

        assertEquals(base, CalculadoraPerfil.firmaTrazado(tramoRecto, TipoRecorrido.LINEAL))
        assertNotEquals(base, movido)
        assertNotEquals(base, deIdaYVuelta)
    }
}
