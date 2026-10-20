package com.unaj.senderos.core.domain.sensores

import com.unaj.senderos.core.domain.model.MuestraAcelerometro
import kotlin.math.sqrt

private const val FRECUENCIA_CORTE_GRAVEDAD_HERCIOS = 0.3
private const val FRECUENCIA_CORTE_SUAVIZADO_HERCIOS = 6.0
private const val UMBRAL_PICO_METROS_POR_SEGUNDO_CUADRADO = 1.2
private const val SEPARACION_MINIMA_MILISEGUNDOS = 250L
private const val MILISEGUNDOS_POR_SEGUNDO = 1000.0

// RF6 - Pasos: restamos la gravedad a la magnitud del acelerómetro (así no importa la orientación) y contamos los picos
// Tras un paso se rearma recién cuando la señal baja de la mitad del umbral, así un paso con dos picos no cuenta doble
class DetectorPasos(
    private val umbral: Double = UMBRAL_PICO_METROS_POR_SEGUNDO_CUADRADO,
    private val separacionMinimaMilisegundos: Long = SEPARACION_MINIMA_MILISEGUNDOS
) {

    private var gravedadEstimada: Double? = null
    private var suavizada = 0.0
    private var instanteAnterior = 0L
    private var anteanterior: Double? = null
    private var anterior: Double? = null
    private var instanteDelAnterior = 0L
    private var instanteUltimoPaso: Long? = null
    private var estaArmado = true

    fun procesar(muestra: MuestraAcelerometro): Boolean {
        val magnitud = sqrt((muestra.x * muestra.x + muestra.y * muestra.y + muestra.z * muestra.z).toDouble())
        val gravedad = gravedadEstimada
        if (gravedad == null) {
            gravedadEstimada = magnitud
            suavizada = 0.0
            instanteAnterior = muestra.instanteMilisegundos
            return false
        }
        val intervaloSegundos = ((muestra.instanteMilisegundos - instanteAnterior) / MILISEGUNDOS_POR_SEGUNDO).coerceAtLeast(0.0)
        instanteAnterior = muestra.instanteMilisegundos
        val nuevaGravedad = gravedad + coeficiente(intervaloSegundos, FRECUENCIA_CORTE_GRAVEDAD_HERCIOS) * (magnitud - gravedad)
        gravedadEstimada = nuevaGravedad
        val dinamica = magnitud - nuevaGravedad
        suavizada += coeficiente(intervaloSegundos, FRECUENCIA_CORTE_SUAVIZADO_HERCIOS) * (dinamica - suavizada)
        return evaluarPico(suavizada, muestra.instanteMilisegundos)
    }

    private fun coeficiente(intervaloSegundos: Double, frecuenciaCorteHercios: Double): Double {
        val constanteDeTiempo = 1.0 / (2.0 * Math.PI * frecuenciaCorteHercios)
        return intervaloSegundos / (constanteDeTiempo + intervaloSegundos)
    }

    private fun evaluarPico(actual: Double, instanteActual: Long): Boolean {
        val candidato = anterior
        val previo = anteanterior
        val instanteCandidato = instanteDelAnterior
        anteanterior = anterior
        anterior = actual
        instanteDelAnterior = instanteActual
        if (!estaArmado && actual < umbral / 2) estaArmado = true
        if (candidato == null || previo == null) return false
        val esPico = estaArmado && candidato > umbral && candidato >= previo && candidato > actual
        val ultimoPaso = instanteUltimoPaso
        val estaSeparado = ultimoPaso == null || instanteCandidato - ultimoPaso >= separacionMinimaMilisegundos
        if (!esPico || !estaSeparado) return false
        instanteUltimoPaso = instanteCandidato
        estaArmado = false
        return true
    }
}
