package com.unaj.senderos.domain.recorrido

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Test

// Una diferencia de 0,0005 grados de latitud equivale a unos 55,6 m; 0,00002 grados, a unos 2,2 m
private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07
private const val INSTANTE_INICIAL = 1_000L
private const val PRECISION_UMBRAL_CON_GPS = 30f
private const val PRECISION_UMBRAL_SIN_GPS = 100f

class CalculadoraTrazadoCaminadoTest {

    private val sendero = crearSenderoDePrueba(
        recorrido = listOf(PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE), PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE))
    ).copy(tipoRecorrido = TipoRecorrido.LINEAL)

    // Construir una coordenada al sur del origen según los grados de latitud indicados
    private fun posicion(gradosAlSur: Double): Coordenada = Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE)

    // Aplicar una lectura con la precisión y el umbral indicados en el instante dado
    private fun aplicar(
        estado: EstadoSeguimiento,
        gradosAlSur: Double,
        precision: Float?,
        instante: Long,
        umbral: Float = PRECISION_UMBRAL_CON_GPS
    ): EstadoSeguimiento {
        val lectura = LecturaUbicacion(posicion(gradosAlSur), null, null, precision)
        return CalculadoraSeguimiento.actualizar(estado, lectura, instante, umbral).estado
    }

    // Construir el estado inicial del seguimiento del sendero de prueba
    private fun estadoInicial(): EstadoSeguimiento = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

    // Verificar que una lectura imprecisa dentro del doble del umbral entra al trazado caminado
    @Test
    fun `una lectura imprecisa dentro del doble del umbral entra al trazado caminado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(primera, 0.0005, 50f, 4_000L)

        assertEquals(listOf(posicion(0.0), posicion(0.0005)), estado.trazadoCaminado)
    }

    // Verificar que el límite del trazado es inclusivo en 60 m y que una lectura peor se excluye
    @Test
    fun `una lectura peor que el doble del umbral queda fuera del trazado caminado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val enElLimite = aplicar(primera, 0.0005, 60f, 4_000L)
        val peor = aplicar(enElLimite, 0.001, 61f, 6_000L)

        assertEquals(2, enElLimite.trazadoCaminado.size)
        assertEquals(enElLimite.trazadoCaminado, peor.trazadoCaminado)
    }

    // Verificar que sin GPS el límite del trazado es el doble de 100 m
    @Test
    fun `sin GPS el trazado acepta hasta doscientos metros de precision`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L, PRECISION_UMBRAL_SIN_GPS)

        val aceptada = aplicar(primera, 0.0005, 200f, 4_000L, PRECISION_UMBRAL_SIN_GPS)
        val descartada = aplicar(aceptada, 0.001, 201f, 6_000L, PRECISION_UMBRAL_SIN_GPS)

        assertEquals(2, aceptada.trazadoCaminado.size)
        assertEquals(2, descartada.trazadoCaminado.size)
    }

    // Verificar que las lecturas imprecisas del trazado no cambian distancia, posición, tramo ni señal válida
    @Test
    fun `las lecturas imprecisas del trazado no cambian la distancia ni la posicion`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(aplicar(primera, 0.0005, 50f, 4_000L), 0.001, 40f, 6_000L)

        assertEquals(primera.distanciaRecorridaKilometros, estado.distanciaRecorridaKilometros, 0.0)
        assertEquals(primera.distanciaRestanteKilometros, estado.distanciaRestanteKilometros, 0.0)
        assertEquals(primera.posicionActual, estado.posicionActual)
        assertEquals(primera.ultimaPosicionAceptada, estado.ultimaPosicionAceptada)
        assertEquals(primera.indiceTramoActual, estado.indiceTramoActual)
        assertEquals(primera.instanteUltimaLecturaValidaMilisegundos, estado.instanteUltimaLecturaValidaMilisegundos)
        assertEquals(3, estado.trazadoCaminado.size)
    }

    // Verificar que el trazado empieza en la primera lectura recibida aunque sea imprecisa
    @Test
    fun `el trazado empieza en la primera lectura aunque sea imprecisa`() {
        val imprecisa = aplicar(estadoInicial(), 0.0, 50f, 2_000L)
        val valida = aplicar(imprecisa, 0.0005, 5f, 4_000L)

        assertEquals(listOf(posicion(0.0)), imprecisa.trazadoCaminado)
        assertEquals(listOf(posicion(0.0), posicion(0.0005)), valida.trazadoCaminado)
    }

    // Verificar que la primera lectura peor que el doble del umbral no abre el trazado
    @Test
    fun `una primera lectura demasiado imprecisa no abre el trazado`() {
        val estado = aplicar(estadoInicial(), 0.0, 90f, 2_000L)

        assertEquals(emptyList<Coordenada>(), estado.trazadoCaminado)
    }

    // Verificar que la separación mínima de 5 m también rige entre lecturas imprecisas
    @Test
    fun `las lecturas cercanas no se repiten en el trazado`() {
        val primera = aplicar(estadoInicial(), 0.0, 50f, 2_000L)

        val estado = aplicar(primera, 0.00002, 50f, 4_000L)

        assertEquals(1, estado.trazadoCaminado.size)
    }

    // Verificar que una lectura válida cercana tras un hueco de más de 30 s se agrega al trazado
    @Test
    fun `una lectura valida cercana tras un hueco largo se agrega al trazado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(primera, 0.00002, 5f, 33_000L)

        assertEquals(listOf(posicion(0.0), posicion(0.00002)), estado.trazadoCaminado)
    }

    // Verificar que sin hueco largo una lectura válida cercana no se agrega
    @Test
    fun `una lectura valida cercana sin hueco largo no se agrega al trazado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(primera, 0.00002, 5f, 32_000L)

        assertEquals(1, estado.trazadoCaminado.size)
    }

    // Verificar que una lectura imprecisa cercana tras un hueco largo no se agrega
    @Test
    fun `una lectura imprecisa cercana tras un hueco largo no se agrega al trazado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(primera, 0.00002, 50f, 60_000L)

        assertEquals(1, estado.trazadoCaminado.size)
    }

    // Verificar que cerrar el trazado agrega la última posición conocida si dista 5 m o más
    @Test
    fun `cerrar el trazado agrega la ultima posicion conocida`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)
        val conUltima = primera.copy(ultimaPosicionConocida = posicion(0.0005))

        assertEquals(listOf(posicion(0.0), posicion(0.0005)), CalculadoraSeguimiento.cerrarTrazadoCaminado(conUltima))
    }

    // Verificar que cerrar el trazado no repite la última posición si está a menos de 5 m
    @Test
    fun `cerrar el trazado no repite una ultima posicion cercana`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)
        val cercana = aplicar(primera, 0.00002, 50f, 4_000L)

        assertEquals(listOf(posicion(0.0)), CalculadoraSeguimiento.cerrarTrazadoCaminado(cercana))
    }

    // Verificar que la última posición recibida cierra el trazado aunque no se haya agregado antes
    @Test
    fun `el trazado termina en la ultima posicion recibida`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)
        val ultima = aplicar(primera, 0.0005, 50f, 4_000L).copy(trazadoCaminado = primera.trazadoCaminado)

        assertEquals(posicion(0.0005), CalculadoraSeguimiento.cerrarTrazadoCaminado(ultima).last())
    }

    // Verificar que cerrar un estado sin lecturas devuelve el trazado vacío
    @Test
    fun `cerrar un trazado sin lecturas lo deja vacio`() {
        assertEquals(emptyList<Coordenada>(), CalculadoraSeguimiento.cerrarTrazadoCaminado(estadoInicial()))
    }
}
