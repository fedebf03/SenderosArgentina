package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.testing.crearSenderoDePrueba
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

    private fun posicion(gradosAlSur: Double): Coordenada = Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE)

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

    private fun estadoInicial(): EstadoSeguimiento = CalculadoraSeguimiento.iniciar(sendero, INSTANTE_INICIAL)

    @Test
    fun `una lectura imprecisa dentro del limite del trazado entra al trazado caminado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(primera, 0.0005, 50f, 4_000L)

        assertEquals(listOf(posicion(0.0), posicion(0.0005)), estado.trazadoCaminado)
    }

    // Con GPS el límite del trazado es 50 m (el doble del umbral, con tope de 50 m), inclusivo
    @Test
    fun `una lectura peor que el limite del trazado queda fuera del trazado caminado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val enElLimite = aplicar(primera, 0.0005, 50f, 4_000L)
        val peor = aplicar(enElLimite, 0.001, 51f, 6_000L)

        assertEquals(2, enElLimite.trazadoCaminado.size)
        assertEquals(enElLimite.trazadoCaminado, peor.trazadoCaminado)
    }

    // Sin GPS el umbral de 100 m ya supera el tope: solo entran las lecturas válidas
    @Test
    fun `sin GPS el trazado acepta solo hasta el umbral de cien metros`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L, PRECISION_UMBRAL_SIN_GPS)

        val aceptada = aplicar(primera, 0.0005, 100f, 4_000L, PRECISION_UMBRAL_SIN_GPS)
        val descartada = aplicar(aceptada, 0.001, 101f, 6_000L, PRECISION_UMBRAL_SIN_GPS)

        assertEquals(2, aceptada.trazadoCaminado.size)
        assertEquals(2, descartada.trazadoCaminado.size)
    }

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

    @Test
    fun `el trazado empieza en la primera lectura aunque sea imprecisa`() {
        val imprecisa = aplicar(estadoInicial(), 0.0, 50f, 2_000L)
        val valida = aplicar(imprecisa, 0.0005, 5f, 4_000L)

        assertEquals(listOf(posicion(0.0)), imprecisa.trazadoCaminado)
        assertEquals(listOf(posicion(0.0), posicion(0.0005)), valida.trazadoCaminado)
    }

    @Test
    fun `una primera lectura demasiado imprecisa no abre el trazado`() {
        val estado = aplicar(estadoInicial(), 0.0, 51f, 2_000L)

        assertEquals(emptyList<Coordenada>(), estado.trazadoCaminado)
    }

    @Test
    fun `las lecturas cercanas no se repiten en el trazado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(primera, 0.00002, 5f, 4_000L)

        assertEquals(1, estado.trazadoCaminado.size)
    }

    // Unos 33 m de separación con 50 m de precisión: puede ser solo error del GPS con el usuario quieto
    @Test
    fun `una lectura imprecisa mas cerca que su propia precision no entra al trazado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(primera, 0.0003, 50f, 4_000L)

        assertEquals(1, estado.trazadoCaminado.size)
    }

    @Test
    fun `quedarse quieto mucho tiempo no suma puntos repetidos`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)

        val estado = aplicar(aplicar(primera, 0.00002, 5f, 40_000L), 0.0, 5f, 80_000L)

        assertEquals(1, estado.trazadoCaminado.size)
    }

    @Test
    fun `cerrar el trazado agrega la ultima lectura que no habia entrado`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)
        val cercana = aplicar(primera, 0.00002, 5f, 4_000L)

        assertEquals(listOf(posicion(0.0), posicion(0.00002)), CalculadoraSeguimiento.cerrarTrazadoCaminado(cercana))
    }

    @Test
    fun `cerrar el trazado no repite una lectura que ya entro`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)
        val lejana = aplicar(primera, 0.0005, 5f, 4_000L)

        assertEquals(listOf(posicion(0.0), posicion(0.0005)), CalculadoraSeguimiento.cerrarTrazadoCaminado(lejana))
    }

    @Test
    fun `cerrar el trazado ignora una lectura fuera del limite de precision`() {
        val primera = aplicar(estadoInicial(), 0.0, 5f, 2_000L)
        val descartada = aplicar(primera, 0.0005, 90f, 4_000L)

        assertEquals(listOf(posicion(0.0)), CalculadoraSeguimiento.cerrarTrazadoCaminado(descartada))
    }

    @Test
    fun `cerrar un trazado sin lecturas lo deja vacio`() {
        assertEquals(emptyList<Coordenada>(), CalculadoraSeguimiento.cerrarTrazadoCaminado(estadoInicial()))
    }
}
