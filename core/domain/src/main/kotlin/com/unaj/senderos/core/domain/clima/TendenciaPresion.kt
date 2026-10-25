package com.unaj.senderos.core.domain.clima

private const val VENTANA_MILISEGUNDOS = 3L * 60L * 60_000L
private const val DURACION_MINIMA_MILISEGUNDOS = 60L * 60_000L
private const val SEPARACION_MINIMA_MILISEGUNDOS = 5L * 60_000L
private const val SALTO_DE_ALTURA_HECTOPASCALES = 2.0
private const val SALTO_DE_ALTURA_MAXIMA_ESPERA_MILISEGUNDOS = 30L * 60_000L
private const val CAIDA_RAPIDA_HECTOPASCALES = -3.0
private const val CAIDA_FUERTE_HECTOPASCALES = -6.0

data class MuestraPresion(val instanteMilisegundos: Long, val hectopascales: Double)

// La caída de la presión anticipa que el tiempo va a empeorar y se puede medir sin conexión
enum class AvisoPresion(val titulo: String, val detalle: String) {
    CAIDA_RAPIDA(
        "La presión baja rápido",
        "La presión cayó 3 hPa o más en 3 horas: el tiempo puede empeorar. Mirá el cielo y pensá en acortar el recorrido"
    ),
    CAIDA_FUERTE(
        "La presión cae muy rápido",
        "La presión cayó 6 hPa o más en 3 horas: se acerca mal tiempo. Conviene volver o buscar refugio"
    )
}

object EvaluadorTendenciaPresion {

    // La variación se lleva a un período de 3 horas. Si las muestras no cubren al menos una hora devuelve nulo
    fun variacionPorTresHoras(muestras: List<MuestraPresion>): Double? {
        val primera = muestras.firstOrNull() ?: return null
        val ultima = muestras.last()
        val duracion = ultima.instanteMilisegundos - primera.instanteMilisegundos
        if (duracion < DURACION_MINIMA_MILISEGUNDOS) return null
        return (ultima.hectopascales - primera.hectopascales) * VENTANA_MILISEGUNDOS / duracion
    }

    fun evaluar(muestras: List<MuestraPresion>): AvisoPresion? {
        val variacion = variacionPorTresHoras(muestras) ?: return null
        return when {
            variacion <= CAIDA_FUERTE_HECTOPASCALES -> AvisoPresion.CAIDA_FUERTE
            variacion <= CAIDA_RAPIDA_HECTOPASCALES -> AvisoPresion.CAIDA_RAPIDA
            else -> null
        }
    }
}

// Subir o bajar una cuesta cambia la presión mucho más rápido que el clima. Por lo tanto un salto brusco entre
// dos muestras seguidas se toma como cambio de altura y se empieza de nuevo
class RegistroPresion {

    private val muestras = ArrayDeque<MuestraPresion>()

    fun agregar(muestra: MuestraPresion): Boolean {
        val anterior = muestras.lastOrNull()
        if (anterior != null) {
            val espera = muestra.instanteMilisegundos - anterior.instanteMilisegundos
            if (espera < SEPARACION_MINIMA_MILISEGUNDOS) return false
            val salto = kotlin.math.abs(muestra.hectopascales - anterior.hectopascales)
            if (salto > SALTO_DE_ALTURA_HECTOPASCALES && espera <= SALTO_DE_ALTURA_MAXIMA_ESPERA_MILISEGUNDOS) muestras.clear()
        }
        muestras.addLast(muestra)
        while (muestras.isNotEmpty() && muestra.instanteMilisegundos - muestras.first().instanteMilisegundos > VENTANA_MILISEGUNDOS) {
            muestras.removeFirst()
        }
        return true
    }

    fun muestras(): List<MuestraPresion> = muestras.toList()
}
