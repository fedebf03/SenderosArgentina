package com.unaj.senderos.core.domain.clima

import com.unaj.senderos.core.domain.model.Coordenada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Interpretación de los códigos de clima de Open-Meteo y avisos de viento, tormenta, lluvia, frío y calor extremos
class AlertasClimaTest {

    private fun dia(
        fecha: String = "2026-10-19",
        maxima: Double = 18.0,
        minima: Double = 6.0,
        probabilidadLluvia: Int? = 10,
        precipitacion: Double = 0.0,
        viento: Double = 15.0,
        rafagas: Double = 25.0,
        condicion: CondicionClima = CondicionClima.DESPEJADO
    ) = PronosticoDia(fecha, maxima, minima, probabilidadLluvia, precipitacion, viento, rafagas, condicion)

    private fun actual(
        temperatura: Double = 12.0,
        viento: Double = 10.0,
        rafagas: Double? = 18.0,
        condicion: CondicionClima = CondicionClima.DESPEJADO
    ) = ClimaActual(temperatura, null, viento, rafagas, condicion, esDeDia = true)

    @Test
    fun `los codigos de clima se interpretan en condiciones en espanol`() {
        assertEquals(CondicionClima.DESPEJADO, CondicionClima.desdeCodigo(0))
        assertEquals(CondicionClima.POCO_NUBLADO, CondicionClima.desdeCodigo(1))
        assertEquals(CondicionClima.PARCIALMENTE_NUBLADO, CondicionClima.desdeCodigo(2))
        assertEquals(CondicionClima.NUBLADO, CondicionClima.desdeCodigo(3))
        assertEquals(CondicionClima.NIEBLA, CondicionClima.desdeCodigo(45))
        assertEquals(CondicionClima.LLOVIZNA, CondicionClima.desdeCodigo(53))
        assertEquals(CondicionClima.LLUVIA, CondicionClima.desdeCodigo(63))
        assertEquals(CondicionClima.LLUVIA_FUERTE, CondicionClima.desdeCodigo(65))
        assertEquals(CondicionClima.NIEVE, CondicionClima.desdeCodigo(73))
        assertEquals(CondicionClima.CHUBASCOS, CondicionClima.desdeCodigo(81))
        assertEquals(CondicionClima.TORMENTA, CondicionClima.desdeCodigo(95))
        assertEquals(CondicionClima.TORMENTA_CON_GRANIZO, CondicionClima.desdeCodigo(99))
    }

    @Test
    fun `un codigo desconocido no rompe y se muestra como variable`() {
        assertEquals(CondicionClima.VARIABLE, CondicionClima.desdeCodigo(123))
        assertEquals(CondicionClima.VARIABLE, CondicionClima.desdeCodigo(-1))
    }

    @Test
    fun `cada condicion tiene una descripcion para mostrar`() {
        CondicionClima.entries.forEach { condicion -> assertTrue(condicion.descripcion.isNotBlank()) }
        assertEquals("Tormenta", CondicionClima.TORMENTA.descripcion)
    }

    @Test
    fun `un dia tranquilo no tiene avisos`() {
        assertTrue(EvaluadorAlertasClima.evaluarDia(dia()).isEmpty())
    }

    @Test
    fun `el viento sostenido de 40 kilometros por hora o mas avisa viento fuerte`() {
        assertTrue(AlertaClima.VIENTO_FUERTE in EvaluadorAlertasClima.evaluarDia(dia(viento = 40.0)))
        assertTrue(AlertaClima.VIENTO_FUERTE !in EvaluadorAlertasClima.evaluarDia(dia(viento = 39.9, rafagas = 55.0)))
    }

    @Test
    fun `las rafagas de 60 kilometros por hora o mas avisan viento fuerte aunque el viento sostenido sea moderado`() {
        assertTrue(AlertaClima.VIENTO_FUERTE in EvaluadorAlertasClima.evaluarDia(dia(viento = 20.0, rafagas = 60.0)))
    }

    @Test
    fun `una tormenta con o sin granizo avisa tormenta`() {
        assertTrue(AlertaClima.TORMENTA in EvaluadorAlertasClima.evaluarDia(dia(condicion = CondicionClima.TORMENTA)))
        assertTrue(AlertaClima.TORMENTA in EvaluadorAlertasClima.evaluarDia(dia(condicion = CondicionClima.TORMENTA_CON_GRANIZO)))
    }

    @Test
    fun `20 milimetros o mas de lluvia avisan lluvia intensa`() {
        assertTrue(AlertaClima.LLUVIA_INTENSA in EvaluadorAlertasClima.evaluarDia(dia(precipitacion = 20.0)))
        assertTrue(AlertaClima.LLUVIA_INTENSA !in EvaluadorAlertasClima.evaluarDia(dia(precipitacion = 19.9)))
    }

    @Test
    fun `la lluvia fuerte avisa lluvia intensa aunque no haya milimetros informados`() {
        assertTrue(AlertaClima.LLUVIA_INTENSA in EvaluadorAlertasClima.evaluarDia(dia(condicion = CondicionClima.LLUVIA_FUERTE)))
    }

    @Test
    fun `una minima de menos 5 grados o menos avisa frio extremo`() {
        assertTrue(AlertaClima.FRIO_EXTREMO in EvaluadorAlertasClima.evaluarDia(dia(minima = -5.0)))
        assertTrue(AlertaClima.FRIO_EXTREMO !in EvaluadorAlertasClima.evaluarDia(dia(minima = -4.9)))
    }

    @Test
    fun `una maxima de 35 grados o mas avisa calor extremo`() {
        assertTrue(AlertaClima.CALOR_EXTREMO in EvaluadorAlertasClima.evaluarDia(dia(maxima = 35.0)))
        assertTrue(AlertaClima.CALOR_EXTREMO !in EvaluadorAlertasClima.evaluarDia(dia(maxima = 34.9)))
    }

    @Test
    fun `un dia puede juntar varios avisos`() {
        val avisos = EvaluadorAlertasClima.evaluarDia(dia(viento = 45.0, condicion = CondicionClima.TORMENTA, precipitacion = 30.0))

        assertEquals(setOf(AlertaClima.VIENTO_FUERTE, AlertaClima.TORMENTA, AlertaClima.LLUVIA_INTENSA), avisos)
    }

    @Test
    fun `el clima de ahora tambien avisa viento y tormenta`() {
        assertTrue(AlertaClima.VIENTO_FUERTE in EvaluadorAlertasClima.evaluarActual(actual(viento = 42.0)))
        assertTrue(AlertaClima.VIENTO_FUERTE in EvaluadorAlertasClima.evaluarActual(actual(rafagas = 65.0)))
        assertTrue(AlertaClima.TORMENTA in EvaluadorAlertasClima.evaluarActual(actual(condicion = CondicionClima.TORMENTA)))
        assertTrue(EvaluadorAlertasClima.evaluarActual(actual()).isEmpty())
    }

    @Test
    fun `las rafagas desconocidas no cuentan`() {
        assertTrue(EvaluadorAlertasClima.evaluarActual(actual(rafagas = null)).isEmpty())
    }

    @Test
    fun `los avisos de todo el pronostico se agrupan por dia y los dias tranquilos no aparecen`() {
        val pronostico = PronosticoClima(
            lugar = Coordenada(-41.13, -71.30),
            obtenidoMilisegundos = 1_000L,
            actual = actual(),
            dias = listOf(dia("2026-10-19"), dia("2026-10-20", viento = 50.0), dia("2026-10-21", condicion = CondicionClima.TORMENTA))
        )

        val porDia = EvaluadorAlertasClima.evaluarDias(pronostico)

        assertEquals(setOf("2026-10-20", "2026-10-21"), porDia.keys)
        assertEquals(setOf(AlertaClima.VIENTO_FUERTE), porDia["2026-10-20"])
    }

    @Test
    fun `cada aviso tiene un titulo y un detalle para mostrar`() {
        AlertaClima.entries.forEach { alerta ->
            assertTrue(alerta.titulo.isNotBlank())
            assertTrue(alerta.detalle.isNotBlank())
        }
    }
}
