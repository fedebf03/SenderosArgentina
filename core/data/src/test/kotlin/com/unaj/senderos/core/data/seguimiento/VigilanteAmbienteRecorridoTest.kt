package com.unaj.senderos.core.data.seguimiento

import com.unaj.senderos.core.domain.clima.AlertaClima
import com.unaj.senderos.core.domain.clima.AvisoPresion
import com.unaj.senderos.core.domain.clima.ClimaActual
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.port.FuentePresion
import com.unaj.senderos.core.domain.port.Reloj
import com.unaj.senderos.core.domain.usecase.ResultadoPronostico
import com.unaj.senderos.core.testing.crearPronosticoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTO = 60_000L
private const val HORA = 60 * MINUTO

// Vigilar el clima y la presión durante el recorrido y avisar una sola vez cada novedad
@OptIn(ExperimentalCoroutinesApi::class)
class VigilanteAmbienteRecorridoTest {

    private val estado = MutableStateFlow<EstadoSeguimiento?>(null)
    private val presion = MutableStateFlow<Double?>(1013.0)
    private val avisos = mutableListOf<AvisoAmbiente>()
    private var pronostico = crearPronosticoDePrueba()
    private var pedidos = 0
    private var haySinBarometro = false

    private fun crearEstado(detenido: Boolean, posicion: Coordenada? = Coordenada(-41.13, -71.30)) = EstadoSeguimiento(
        idSendero = "libre-1",
        trazado = emptyList(),
        posicionActual = posicion,
        estaDetenido = detenido
    )

    private fun TestScope.crearVigilante() = VigilanteAmbienteRecorrido(
        alcance = backgroundScope,
        estado = estado,
        fuentePresion = object : FuentePresion {
            override fun observarPresion(): Flow<Double> = flow {
                if (haySinBarometro) throw IllegalStateException("Sin barómetro")
                presion.collect { valor -> if (valor != null) emit(valor) }
            }
        },
        obtenerPronostico = { _ ->
            pedidos++
            Result.success(ResultadoPronostico(pronostico, 0L, estaDesactualizado = false, mensajeError = null))
        },
        reloj = Reloj { currentTime },
        alAvisar = { aviso -> avisos.add(aviso) }
    )

    @Test
    fun `una tormenta en el lugar se avisa una sola vez aunque se renueve el pronostico`() = runTest {
        pronostico = crearPronosticoDePrueba(actual = ClimaActual(12.0, 10.0, 15.0, 25.0, CondicionClima.TORMENTA, esDeDia = true))
        estado.value = crearEstado(detenido = false)
        crearVigilante().iniciar()

        runCurrent()
        advanceTimeBy(25 * MINUTO)

        assertTrue(pedidos >= 3)
        assertEquals(listOf<AvisoAmbiente>(AvisoAmbiente.Clima(AlertaClima.TORMENTA)), avisos)
    }

    @Test
    fun `sin posicion no pide el clima`() = runTest {
        estado.value = crearEstado(detenido = false, posicion = null)
        crearVigilante().iniciar()

        runCurrent()
        advanceTimeBy(MINUTO)

        assertEquals(0, pedidos)
    }

    @Test
    fun `un dia tranquilo no avisa nada`() = runTest {
        estado.value = crearEstado(detenido = false)
        crearVigilante().iniciar()

        runCurrent()
        advanceTimeBy(HORA)

        assertTrue(avisos.isEmpty())
    }

    @Test
    fun `una caida de presion con el usuario detenido avisa y sube de nivel si sigue cayendo`() = runTest {
        estado.value = crearEstado(detenido = true)
        crearVigilante().iniciar()
        runCurrent()
        // 1013 a 1010 en 3 horas (caída rápida), anotando cada 5 minutos
        for (minuto in 1..36) {
            presion.value = 1013.0 - 3.0 * minuto / 36
            advanceTimeBy(5 * MINUTO)
        }
        assertEquals(listOf<AvisoAmbiente>(AvisoAmbiente.Presion(AvisoPresion.CAIDA_RAPIDA)), avisos)

        for (minuto in 1..36) {
            presion.value = 1010.0 - 7.0 * minuto / 36
            advanceTimeBy(5 * MINUTO)
        }

        assertEquals(AvisoAmbiente.Presion(AvisoPresion.CAIDA_FUERTE), avisos.last())
        assertEquals(2, avisos.size)
    }

    @Test
    fun `caminando no se anota la presion`() = runTest {
        estado.value = crearEstado(detenido = false)
        crearVigilante().iniciar()
        runCurrent()

        for (minuto in 1..36) {
            presion.value = 1013.0 - 8.0 * minuto / 36
            advanceTimeBy(5 * MINUTO)
        }

        assertTrue(avisos.none { aviso -> aviso is AvisoAmbiente.Presion })
    }

    @Test
    fun `sin barometro el clima sigue funcionando`() = runTest {
        haySinBarometro = true
        pronostico = crearPronosticoDePrueba(actual = ClimaActual(12.0, 10.0, 15.0, 25.0, CondicionClima.TORMENTA, esDeDia = true))
        estado.value = crearEstado(detenido = true)
        crearVigilante().iniciar()

        runCurrent()
        advanceTimeBy(HORA)

        assertEquals(listOf<AvisoAmbiente>(AvisoAmbiente.Clima(AlertaClima.TORMENTA)), avisos)
    }

    @Test
    fun `al detener deja de vigilar`() = runTest {
        estado.value = crearEstado(detenido = false)
        val vigilante = crearVigilante()
        vigilante.iniciar()
        runCurrent()
        val pedidosAntes = pedidos

        vigilante.detener()
        advanceTimeBy(HORA)

        assertEquals(pedidosAntes, pedidos)
    }
}
