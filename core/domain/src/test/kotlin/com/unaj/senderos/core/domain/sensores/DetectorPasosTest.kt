package com.unaj.senderos.domain.sensores

import com.unaj.senderos.domain.model.MuestraAcelerometro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

private const val GRAVEDAD = 9.81
private const val INTERVALO_MILISEGUNDOS = 20L

class DetectorPasosTest {

    private fun contarPasos(frecuenciaHercios: Double, amplitud: Double, duracionMilisegundos: Long): Int {
        val detector = DetectorPasos()
        var pasos = 0
        var instante = 0L
        while (instante <= duracionMilisegundos) {
            val z = GRAVEDAD + amplitud * sin(2 * PI * frecuenciaHercios * instante / 1000.0)
            if (detector.procesar(MuestraAcelerometro(0f, 0f, z.toFloat(), instante))) pasos++
            instante += INTERVALO_MILISEGUNDOS
        }
        return pasos
    }

    private fun contarPasosConPicosEn(vararg indicesDePico: Int): Int {
        val detector = DetectorPasos()
        var pasos = 0
        for (indice in 0 until 100) {
            val z = if (indice in indicesDePico) GRAVEDAD + 4.0 else GRAVEDAD
            if (detector.procesar(MuestraAcelerometro(0f, 0f, z.toFloat(), indice * INTERVALO_MILISEGUNDOS))) pasos++
        }
        return pasos
    }

    @Test
    fun `una caminata de dos pasos por segundo cuenta los pasos`() {
        val pasos = contarPasos(frecuenciaHercios = 2.0, amplitud = 3.0, duracionMilisegundos = 10_000L)

        assertTrue("Pasos: $pasos", pasos in 18..21)
    }

    @Test
    fun `el ruido pequeno no cuenta pasos`() {
        val detector = DetectorPasos()
        var pasos = 0
        var semilla = 12345L
        for (indice in 0 until 500) {
            semilla = (semilla * 1103515245L + 12345L) and 0x7fffffffL
            val ruido = (semilla % 1000) / 1000.0 * 0.3 - 0.15
            val muestra = MuestraAcelerometro(0f, 0f, (GRAVEDAD + ruido).toFloat(), indice * INTERVALO_MILISEGUNDOS)
            if (detector.procesar(muestra)) pasos++
        }

        assertEquals(0, pasos)
    }

    @Test
    fun `los picos demasiado juntos no duplican pasos`() {
        val pasos = contarPasos(frecuenciaHercios = 8.0, amplitud = 6.0, duracionMilisegundos = 2_000L)

        assertTrue("Pasos: $pasos", pasos in 1..8)
    }

    @Test
    fun `la gravedad constante en cualquier eje no cuenta pasos`() {
        val detector = DetectorPasos()
        var pasos = 0
        for (indice in 0 until 300) {
            if (detector.procesar(MuestraAcelerometro(6.0f, 0f, 7.76f, indice * INTERVALO_MILISEGUNDOS))) pasos++
        }

        assertEquals(0, pasos)
    }

    @Test
    fun `dos pasos separados por mas de 250 ms se cuentan`() {
        assertEquals(2, contarPasosConPicosEn(30, 50))
    }

    @Test
    fun `dos picos a 100 ms cuentan como uno`() {
        assertEquals(1, contarPasosConPicosEn(30, 35))
    }

    // Histéresis: la señal no baja de la mitad del umbral entre los dos picos
    @Test
    fun `dos picos a 300 ms sin bajar de la mitad del umbral cuentan como uno`() {
        val detector = DetectorPasos()
        var pasos = 0
        for (indice in 0 until 120) {
            val meseta = if (indice in 28..52) 3.0 else 0.0
            val pico = if (indice == 30 || indice == 45) 1.5 else 0.0
            val z = GRAVEDAD + meseta + pico
            if (detector.procesar(MuestraAcelerometro(0f, 0f, z.toFloat(), indice * INTERVALO_MILISEGUNDOS))) pasos++
        }

        assertEquals(1, pasos)
    }
}
