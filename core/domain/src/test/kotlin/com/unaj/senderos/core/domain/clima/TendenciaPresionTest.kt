package com.unaj.senderos.core.domain.clima

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTO = 60_000L
private const val HORA = 60 * MINUTO

// Avisar cuando la presión cae rápido, sin confundir una subida o bajada de cuesta con un cambio de tiempo
class TendenciaPresionTest {

    private fun muestras(vararg pares: Pair<Long, Double>) = pares.map { (instante, presion) -> MuestraPresion(instante, presion) }

    @Test
    fun `con menos de una hora de datos no hay conclusion`() {
        val lista = muestras(0L to 1013.0, 30 * MINUTO to 1008.0)

        assertNull(EvaluadorTendenciaPresion.variacionPorTresHoras(lista))
        assertNull(EvaluadorTendenciaPresion.evaluar(lista))
    }

    @Test
    fun `una presion estable no avisa`() {
        assertNull(EvaluadorTendenciaPresion.evaluar(muestras(0L to 1013.0, 3 * HORA to 1012.5)))
    }

    @Test
    fun `una caida de 3 hPa en 3 horas da el aviso de caida rapida`() {
        assertEquals(AvisoPresion.CAIDA_RAPIDA, EvaluadorTendenciaPresion.evaluar(muestras(0L to 1013.0, 3 * HORA to 1010.0)))
    }

    @Test
    fun `una caida de 6 hPa en 3 horas da el aviso de caida fuerte`() {
        assertEquals(AvisoPresion.CAIDA_FUERTE, EvaluadorTendenciaPresion.evaluar(muestras(0L to 1013.0, 3 * HORA to 1007.0)))
    }

    @Test
    fun `la caida de menos de 3 horas se lleva a un periodo de 3 horas`() {
        val variacion = EvaluadorTendenciaPresion.variacionPorTresHoras(muestras(0L to 1013.0, 90 * MINUTO to 1011.0))

        assertEquals(-4.0, variacion ?: 0.0, 0.001)
    }

    @Test
    fun `una subida de presion no avisa`() {
        assertNull(EvaluadorTendenciaPresion.evaluar(muestras(0L to 1005.0, 3 * HORA to 1013.0)))
    }

    @Test
    fun `el registro ignora muestras separadas por menos de 5 minutos`() {
        val registro = RegistroPresion()

        assertTrue(registro.agregar(MuestraPresion(0L, 1013.0)))
        assertFalse(registro.agregar(MuestraPresion(2 * MINUTO, 1013.1)))
        assertEquals(1, registro.muestras().size)
    }

    @Test
    fun `el registro descarta lo que tiene mas de 3 horas`() {
        val registro = RegistroPresion()
        registro.agregar(MuestraPresion(0L, 1013.0))
        registro.agregar(MuestraPresion(2 * HORA, 1012.0))

        registro.agregar(MuestraPresion(4 * HORA, 1011.0))

        assertEquals(listOf(2 * HORA, 4 * HORA), registro.muestras().map { muestra -> muestra.instanteMilisegundos })
    }

    @Test
    fun `un salto brusco por cambio de altura reinicia el registro`() {
        val registro = RegistroPresion()
        registro.agregar(MuestraPresion(0L, 1013.0))
        registro.agregar(MuestraPresion(10 * MINUTO, 1012.8))

        registro.agregar(MuestraPresion(20 * MINUTO, 1000.0))

        assertEquals(listOf(1000.0), registro.muestras().map { muestra -> muestra.hectopascales })
    }
}
