package com.unaj.senderos.domain.recorrido

import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07
private const val INSTANTE_INICIAL = 1_000L

class CalidadSenalTest {

    private fun estadoInicial(): EstadoSeguimiento {
        val puntos = listOf(0.0, 0.001, 0.002).map { grados -> PuntoRecorrido(LATITUD_BASE - grados, LONGITUD_BASE) }
        val sendero = crearSenderoDePrueba(recorrido = puntos).copy(tipoRecorrido = TipoRecorrido.LINEAL)
        return CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)
    }

    private fun lectura(gradosAlSur: Double, precision: Float?): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE), null, null, precision)
    }

    @Test
    fun `el estado inicial esta buscando senal`() {
        val estado = estadoInicial()

        assertEquals(CalidadSenal.BUSCANDO, estado.calidadSenal)
        assertNull(estado.instanteUltimaLecturaValidaMilisegundos)
    }

    @Test
    fun `la primera lectura valida pasa de buscando a buena`() {
        val resultado = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 3_000L)

        assertEquals(CalidadSenal.BUENA, resultado.estado.calidadSenal)
        assertEquals(INSTANTE_INICIAL + 3_000L, resultado.estado.instanteUltimaLecturaValidaMilisegundos)
    }

    @Test
    fun `una lectura sin precision o de treinta metros es valida`() {
        val sinPrecision = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, null), INSTANTE_INICIAL + 1_000L)
        val treintaMetros = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 30f), INSTANTE_INICIAL + 1_000L)

        assertEquals(CalidadSenal.BUENA, sinPrecision.estado.calidadSenal)
        assertEquals(CalidadSenal.BUENA, treintaMetros.estado.calidadSenal)
    }

    @Test
    fun `la senal pasa de buena a debil pasados quince segundos`() {
        val estado = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado
        val ultima = INSTANTE_INICIAL + 1_000L

        assertEquals(CalidadSenal.BUENA, CalculadoraSeguimiento.evaluarCalidad(estado, ultima + 15_000L))
        assertEquals(CalidadSenal.DEBIL, CalculadoraSeguimiento.evaluarCalidad(estado, ultima + 15_001L))
    }

    @Test
    fun `sin lecturas validas busca 30 segundos y luego es debil`() {
        val estado = estadoInicial()

        assertEquals(CalidadSenal.BUSCANDO, CalculadoraSeguimiento.evaluarCalidad(estado, INSTANTE_INICIAL + 30_000L))
        assertEquals(CalidadSenal.DEBIL, CalculadoraSeguimiento.evaluarCalidad(estado, INSTANTE_INICIAL + 30_001L))
    }

    @Test
    fun `una lectura valida vuelve de debil a buena`() {
        val debil = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado
            .copy(calidadSenal = CalidadSenal.DEBIL)

        val resultado = CalculadoraSeguimiento.actualizar(debil, lectura(0.0001, 10f), INSTANTE_INICIAL + 60_000L)

        assertEquals(CalidadSenal.BUENA, resultado.estado.calidadSenal)
    }

    // Dentro del doble del umbral de precisión, la lectura imprecisa sí se agrega al trazado caminado
    @Test
    fun `una lectura imprecisa no mueve la posicion`() {
        val primera = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado

        val imprecisa = CalculadoraSeguimiento.actualizar(primera, lectura(0.0015, 31f), INSTANTE_INICIAL + 5_000L).estado

        assertEquals(primera.posicionActual, imprecisa.posicionActual)
        assertEquals(primera.trazadoCaminado + lectura(0.0015, 31f).coordenada, imprecisa.trazadoCaminado)
        assertEquals(0.0, imprecisa.distanciaRecorridaKilometros, 0.0001)
        assertEquals(primera.instanteUltimaLecturaValidaMilisegundos, imprecisa.instanteUltimaLecturaValidaMilisegundos)
    }

    @Test
    fun `las lecturas imprecisas dejan la senal debil pasados quince segundos`() {
        val primera = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado

        val imprecisa = CalculadoraSeguimiento.actualizar(primera, lectura(0.0015, 80f), INSTANTE_INICIAL + 20_000L).estado

        assertEquals(CalidadSenal.DEBIL, imprecisa.calidadSenal)
    }

    @Test
    fun `una lectura imprecisa al comienzo sigue buscando`() {
        val resultado = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 80f), INSTANTE_INICIAL + 5_000L)

        assertEquals(CalidadSenal.BUSCANDO, resultado.estado.calidadSenal)
        assertNull(resultado.estado.posicionActual)
    }

    @Test
    fun `un recorrido simulado siempre tiene buena senal`() {
        val simulado = estadoInicial().copy(esSimulado = true)

        assertEquals(CalidadSenal.BUENA, CalculadoraSeguimiento.evaluarCalidad(simulado, INSTANTE_INICIAL + 600_000L))
    }
}
