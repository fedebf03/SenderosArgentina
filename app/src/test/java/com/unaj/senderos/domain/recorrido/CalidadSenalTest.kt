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

    // Construir el estado inicial de un sendero lineal de tres puntos separados 0,001 grados
    private fun estadoInicial(): EstadoSeguimiento {
        val puntos = listOf(0.0, 0.001, 0.002).map { grados -> PuntoRecorrido(LATITUD_BASE - grados, LONGITUD_BASE) }
        val sendero = crearSenderoDePrueba(recorrido = puntos).copy(tipoRecorrido = TipoRecorrido.LINEAL)
        return CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)
    }

    // Construir una lectura al sur del origen con la precisión indicada
    private fun lectura(gradosAlSur: Double, precision: Float?): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE), null, null, precision)
    }

    // Verificar que el estado inicial busca señal y no tiene lectura válida
    @Test
    fun `el estado inicial esta buscando senal`() {
        val estado = estadoInicial()

        assertEquals(CalidadSenal.BUSCANDO, estado.calidadSenal)
        assertNull(estado.instanteUltimaLecturaValidaMilisegundos)
    }

    // Verificar la transición de BUSCANDO a BUENA con la primera lectura válida
    @Test
    fun `la primera lectura valida pasa de buscando a buena`() {
        val resultado = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 3_000L)

        assertEquals(CalidadSenal.BUENA, resultado.estado.calidadSenal)
        assertEquals(INSTANTE_INICIAL + 3_000L, resultado.estado.instanteUltimaLecturaValidaMilisegundos)
    }

    // Verificar que una lectura sin precisión informada y una de exactamente 30 m son válidas
    @Test
    fun `una lectura sin precision o de treinta metros es valida`() {
        val sinPrecision = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, null), INSTANTE_INICIAL + 1_000L)
        val treintaMetros = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 30f), INSTANTE_INICIAL + 1_000L)

        assertEquals(CalidadSenal.BUENA, sinPrecision.estado.calidadSenal)
        assertEquals(CalidadSenal.BUENA, treintaMetros.estado.calidadSenal)
    }

    // Verificar que a los 15 s exactos de la última lectura válida la señal sigue buena y pasado ese tiempo es débil
    @Test
    fun `la senal pasa de buena a debil pasados quince segundos`() {
        val estado = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado
        val ultima = INSTANTE_INICIAL + 1_000L

        assertEquals(CalidadSenal.BUENA, CalculadoraSeguimiento.evaluarCalidad(estado, ultima + 15_000L))
        assertEquals(CalidadSenal.DEBIL, CalculadoraSeguimiento.evaluarCalidad(estado, ultima + 15_001L))
    }

    // Verificar que sin ninguna lectura válida busca hasta los 30 s del inicio y después es débil
    @Test
    fun `sin lecturas validas busca 30 segundos y luego es debil`() {
        val estado = estadoInicial()

        assertEquals(CalidadSenal.BUSCANDO, CalculadoraSeguimiento.evaluarCalidad(estado, INSTANTE_INICIAL + 30_000L))
        assertEquals(CalidadSenal.DEBIL, CalculadoraSeguimiento.evaluarCalidad(estado, INSTANTE_INICIAL + 30_001L))
    }

    // Verificar que una lectura válida devuelve la señal a buena desde débil
    @Test
    fun `una lectura valida vuelve de debil a buena`() {
        val debil = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado
            .copy(calidadSenal = CalidadSenal.DEBIL)

        val resultado = CalculadoraSeguimiento.actualizar(debil, lectura(0.0001, 10f), INSTANTE_INICIAL + 60_000L)

        assertEquals(CalidadSenal.BUENA, resultado.estado.calidadSenal)
    }

    // Verificar que una lectura imprecisa no mueve la posición ni suma distancia, aunque dentro del doble del umbral sí se agrega al trazado caminado
    @Test
    fun `una lectura imprecisa no mueve la posicion`() {
        val primera = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado

        val imprecisa = CalculadoraSeguimiento.actualizar(primera, lectura(0.0015, 31f), INSTANTE_INICIAL + 5_000L).estado

        assertEquals(primera.posicionActual, imprecisa.posicionActual)
        assertEquals(primera.trazadoCaminado + lectura(0.0015, 31f).coordenada, imprecisa.trazadoCaminado)
        assertEquals(0.0, imprecisa.distanciaRecorridaKilometros, 0.0001)
        assertEquals(primera.instanteUltimaLecturaValidaMilisegundos, imprecisa.instanteUltimaLecturaValidaMilisegundos)
    }

    // Verificar que las lecturas imprecisas no renuevan la señal: pasados 15 s queda débil
    @Test
    fun `las lecturas imprecisas dejan la senal debil pasados quince segundos`() {
        val primera = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 10f), INSTANTE_INICIAL + 1_000L).estado

        val imprecisa = CalculadoraSeguimiento.actualizar(primera, lectura(0.0015, 80f), INSTANTE_INICIAL + 20_000L).estado

        assertEquals(CalidadSenal.DEBIL, imprecisa.calidadSenal)
    }

    // Verificar que una lectura imprecisa temprana sigue buscando y no sale de BUSCANDO
    @Test
    fun `una lectura imprecisa al comienzo sigue buscando`() {
        val resultado = CalculadoraSeguimiento.actualizar(estadoInicial(), lectura(0.0, 80f), INSTANTE_INICIAL + 5_000L)

        assertEquals(CalidadSenal.BUSCANDO, resultado.estado.calidadSenal)
        assertNull(resultado.estado.posicionActual)
    }

    // Verificar que un recorrido simulado siempre tiene buena señal
    @Test
    fun `un recorrido simulado siempre tiene buena senal`() {
        val simulado = estadoInicial().copy(esSimulado = true)

        assertEquals(CalidadSenal.BUENA, CalculadoraSeguimiento.evaluarCalidad(simulado, INSTANTE_INICIAL + 600_000L))
    }
}
