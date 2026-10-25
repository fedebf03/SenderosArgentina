package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Rectangulo
import com.unaj.senderos.core.domain.model.tieneCoordenadasValidas
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

private const val METROS_POR_GRADO_LATITUD = 111195.0
private const val FACTOR_REDONDEO_FIRMA = 1_000_000.0
private const val BASE_FNV = -3750763034362895579L
private const val PRIMO_FNV = 1099511628211L
private const val COSENO_MINIMO = 1e-6
private const val LARGO_MAXIMO_SUBTRAMO_METROS = 2000.0
private const val METROS_POR_KILOMETRO = 1000.0

object CalculadoraCorredor {
    const val ANCHO_PREDETERMINADO_METROS = 1000.0

    fun rectangulos(puntos: List<PuntoRecorrido>, anchoMetros: Double = ANCHO_PREDETERMINADO_METROS): List<Rectangulo> {
        val puntosValidos = puntos.filter { punto -> Coordenada(punto.latitud, punto.longitud).tieneCoordenadasValidas() }
        if (puntosValidos.isEmpty()) return emptyList()
        if (puntosValidos.size == 1) return listOf(ampliar(puntosValidos[0], puntosValidos[0], anchoMetros))
        return puntosValidos.zipWithNext().flatMap { (origen, destino) ->
            subdividir(origen, destino).map { (inicio, fin) -> ampliar(inicio, fin, anchoMetros) }
        }
    }

    // Los tramos largos se parten cada 2 km como máximo para que sus rectángulos no cubran áreas enormes
    private fun subdividir(origen: PuntoRecorrido, destino: PuntoRecorrido): List<Pair<PuntoRecorrido, PuntoRecorrido>> {
        val largoMetros = CalculadoraRecorrido.distanciaEntre(Coordenada(origen.latitud, origen.longitud), destino) * METROS_POR_KILOMETRO
        val cantidad = kotlin.math.ceil(largoMetros / LARGO_MAXIMO_SUBTRAMO_METROS).toInt().coerceAtLeast(1)
        val extremos = (0..cantidad).map { indice ->
            val fraccion = indice.toDouble() / cantidad
            PuntoRecorrido(
                latitud = origen.latitud + (destino.latitud - origen.latitud) * fraccion,
                longitud = origen.longitud + (destino.longitud - origen.longitud) * fraccion
            )
        }
        return extremos.zipWithNext()
    }

    // La firma es estable y cambia con el orden de los puntos o con las coordenadas hasta el sexto decimal
    fun firmaTrazado(puntos: List<PuntoRecorrido>): String {
        val texto = puntos
            .filter { punto -> Coordenada(punto.latitud, punto.longitud).tieneCoordenadasValidas() }
            .joinToString(";") { punto ->
                "${redondearSeisDecimales(punto.latitud)},${redondearSeisDecimales(punto.longitud)}"
            }
        var hash = BASE_FNV
        texto.forEach { caracter ->
            hash = (hash xor caracter.code.toLong()) * PRIMO_FNV
        }
        return java.lang.Long.toHexString(hash)
    }

    private fun redondearSeisDecimales(valor: Double): Long {
        return (valor * FACTOR_REDONDEO_FIRMA).roundToLong()
    }

    private fun ampliar(origen: PuntoRecorrido, destino: PuntoRecorrido, anchoMetros: Double): Rectangulo {
        val ampliacionLatitud = anchoMetros / METROS_POR_GRADO_LATITUD
        val latitudMasAlejada = max(abs(origen.latitud), abs(destino.latitud))
        val cosenoLatitud = max(cos(Math.toRadians(min(latitudMasAlejada, 89.9))), COSENO_MINIMO)
        val ampliacionLongitud = ampliacionLatitud / cosenoLatitud
        return Rectangulo(
            sur = max(min(origen.latitud, destino.latitud) - ampliacionLatitud, -90.0),
            oeste = max(min(origen.longitud, destino.longitud) - ampliacionLongitud, -180.0),
            norte = min(max(origen.latitud, destino.latitud) + ampliacionLatitud, 90.0),
            este = min(max(origen.longitud, destino.longitud) + ampliacionLongitud, 180.0)
        )
    }
}
