package com.unaj.senderos.core.data.seguimiento

import android.util.Log
import com.unaj.senderos.core.domain.clima.AlertaClima
import com.unaj.senderos.core.domain.clima.AvisoPresion
import com.unaj.senderos.core.domain.clima.EvaluadorAlertasClima
import com.unaj.senderos.core.domain.clima.EvaluadorTendenciaPresion
import com.unaj.senderos.core.domain.clima.MuestraPresion
import com.unaj.senderos.core.domain.clima.RegistroPresion
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.port.FuentePresion
import com.unaj.senderos.core.domain.port.Reloj
import com.unaj.senderos.core.domain.usecase.ResultadoPronostico
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AMBIENTE_RECORRIDO"
private const val INTERVALO_CLIMA_MILISEGUNDOS = 10L * 60_000L
private const val ESPERA_SIN_POSICION_MILISEGUNDOS = 15_000L
private const val INTERVALO_PRESION_MILISEGUNDOS = 5L * 60_000L
private const val RECUPERACION_HECTOPASCALES = -1.5

// Avisos del ambiente que le llegan al usuario aunque tenga la pantalla apagada
sealed interface AvisoAmbiente {
    data class Clima(val alerta: AlertaClima) : AvisoAmbiente
    data class Presion(val aviso: AvisoPresion) : AvisoAmbiente
}

// El clima se consulta cada 10 minutos y la presión se anota cada 5 solo con el usuario detenido. Caminando en subida
// o bajada la presión cambia por la altura y no por el clima
class VigilanteAmbienteRecorrido(
    private val alcance: CoroutineScope,
    private val estado: StateFlow<EstadoSeguimiento?>,
    private val fuentePresion: FuentePresion,
    private val obtenerPronostico: suspend (Coordenada) -> Result<ResultadoPronostico>,
    private val reloj: Reloj,
    private val alAvisar: (AvisoAmbiente) -> Unit
) {
    private val trabajos = mutableListOf<Job>()
    private val alertasAvisadas = mutableSetOf<AlertaClima>()
    private val registroPresion = RegistroPresion()
    private var avisoPresionDado: AvisoPresion? = null
    private var ultimaPresion: Double? = null

    fun iniciar() {
        trabajos += alcance.launch { vigilarClima() }
        trabajos += alcance.launch { recolectarPresion() }
        trabajos += alcance.launch { anotarPresionPeriodicamente() }
    }

    fun detener() {
        trabajos.forEach { trabajo -> trabajo.cancel() }
        trabajos.clear()
    }

    private suspend fun vigilarClima() {
        while (true) {
            val posicion = estado.value?.posicionActual
            if (posicion == null) {
                delay(ESPERA_SIN_POSICION_MILISEGUNDOS)
                continue
            }
            val resultado = try {
                obtenerPronostico(posicion)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            resultado.fold(
                onSuccess = { datos -> avisarAlertasNuevas(datos) },
                onFailure = { error -> Log.e(TAG, "No se pudo obtener el clima para vigilarlo", error) }
            )
            delay(INTERVALO_CLIMA_MILISEGUNDOS)
        }
    }

    private fun avisarAlertasNuevas(datos: ResultadoPronostico) {
        val pronostico = datos.pronostico
        val alertas = EvaluadorAlertasClima.evaluarActual(pronostico.actual) +
            pronostico.dias.firstOrNull()?.let { dia -> EvaluadorAlertasClima.evaluarDia(dia) }.orEmpty()
        alertas.filter { alerta -> alertasAvisadas.add(alerta) }.forEach { alerta -> alAvisar(AvisoAmbiente.Clima(alerta)) }
    }

    private suspend fun recolectarPresion() {
        fuentePresion.observarPresion()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.d(TAG, "Sin barómetro: ${error.message}")
            }
            .collect { hectopascales -> ultimaPresion = hectopascales }
    }

    private suspend fun anotarPresionPeriodicamente() {
        while (true) {
            delay(INTERVALO_PRESION_MILISEGUNDOS)
            val presion = ultimaPresion ?: continue
            if (estado.value?.estaDetenido != true) continue
            if (registroPresion.agregar(MuestraPresion(reloj(), presion))) evaluarPresion()
        }
    }

    // El aviso se vuelve a habilitar recién cuando la presión se recupera de verdad. Así no se repite cuando la caída
    // oscila justo en el umbral
    private fun evaluarPresion() {
        val muestras = registroPresion.muestras()
        val aviso = EvaluadorTendenciaPresion.evaluar(muestras)
        if (aviso == null) {
            val variacion = EvaluadorTendenciaPresion.variacionPorTresHoras(muestras)
            if (variacion == null || variacion > RECUPERACION_HECTOPASCALES) avisoPresionDado = null
            return
        }
        val anterior = avisoPresionDado
        if (anterior == null || aviso.ordinal > anterior.ordinal) {
            avisoPresionDado = aviso
            alAvisar(AvisoAmbiente.Presion(aviso))
        }
    }
}
