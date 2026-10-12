package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.MuestraAcelerometro
import kotlin.math.sqrt

private const val FRECUENCIA_CORTE_GRAVEDAD_HERCIOS = 0.3
private const val FRECUENCIA_CORTE_SUAVIZADO_HERCIOS = 6.0
private const val UMBRAL_PICO_METROS_POR_SEGUNDO_CUADRADO = 1.2
private const val SEPARACION_MINIMA_MILISEGUNDOS = 250L
private const val MILISEGUNDOS_POR_SEGUNDO = 1000.0

// Contar pasos con la magnitud del acelerómetro: un filtro paso bajo estima y resta la gravedad (así no importa la orientación),
// otro suaviza el ruido, y se cuenta un paso en cada pico por encima del umbral separado al menos 250 ms del paso anterior.
// Histéresis: tras un paso el detector se rearma solo cuando la señal suavizada baja de la mitad del umbral (evita doble conteo de un paso con dos picos).
// Un pico se confirma una muestra después (cuando la señal empieza a bajar). Es Kotlin puro y mantiene estado entre muestras
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

    // Procesar una muestra y devolver verdadero si con ella se confirmó un paso
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

    // Calcular el coeficiente de un filtro paso bajo de primer orden para el intervalo y la frecuencia de corte dados
    private fun coeficiente(intervaloSegundos: Double, frecuenciaCorteHercios: Double): Double {
        val constanteDeTiempo = 1.0 / (2.0 * Math.PI * frecuenciaCorteHercios)
        return intervaloSegundos / (constanteDeTiempo + intervaloSegundos)
    }

    // Evaluar si la muestra anterior fue un pico confirmado por la muestra actual y registrar el paso
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
