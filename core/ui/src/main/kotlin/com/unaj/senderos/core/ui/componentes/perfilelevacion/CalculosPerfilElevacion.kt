package com.unaj.senderos.core.ui.componentes

import com.unaj.senderos.core.domain.model.PuntoPerfil
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MAXIMO_MARCAS_ALTITUD = 4
private const val MAXIMO_MARCAS_KILOMETROS = 5
private const val RANGO_MINIMO_ALTITUD_METROS = 10.0
private const val HOLGURA_RELATIVA_EJE = 0.05
private const val TOLERANCIA_DIVISION = 1e-9
private const val FACTOR_REDONDEO_KILOMETROS = 1_000_000.0
private val MANTISAS_PASO_REDONDO = listOf(1.0, 2.0, 5.0)

// Escala del gráfico: rango visible de cada eje y marcas legibles
data class EscalaPerfil(
    val kilometrosTotales: Double,
    val altitudMinimaEjeMetros: Double,
    val altitudMaximaEjeMetros: Double,
    val marcasAltitudMetros: List<Int>,
    val marcasKilometros: List<Double>
)

// Calcular la escala de los ejes para que el perfil use todo el alto y las marcas sean números redondos
fun calcularEscalaPerfil(puntos: List<PuntoPerfil>): EscalaPerfil {
    if (puntos.isEmpty()) return EscalaPerfil(0.0, 0.0, 0.0, emptyList(), listOf(0.0))
    val kilometrosTotales = puntos.maxOf { punto -> punto.distanciaKilometros }.coerceAtLeast(0.0)
    var inferior = puntos.minOf { punto -> punto.altitudMetros }
    var superior = puntos.maxOf { punto -> punto.altitudMetros }
    if (superior - inferior < RANGO_MINIMO_ALTITUD_METROS) {
        val centro = (inferior + superior) / 2.0
        inferior = centro - RANGO_MINIMO_ALTITUD_METROS / 2.0
        superior = centro + RANGO_MINIMO_ALTITUD_METROS / 2.0
    }
    val holgura = (superior - inferior) * HOLGURA_RELATIVA_EJE
    val ejeMinimo = inferior - holgura
    val ejeMaximo = superior + holgura
    return EscalaPerfil(
        kilometrosTotales = kilometrosTotales,
        altitudMinimaEjeMetros = ejeMinimo,
        altitudMaximaEjeMetros = ejeMaximo,
        marcasAltitudMetros = elegirMarcasAltitud(ejeMinimo, ejeMaximo),
        marcasKilometros = calcularMarcasKilometros(kilometrosTotales)
    )
}

// Elegir el paso redondo más chico que deja a lo sumo cuatro marcas dentro del eje
private fun elegirMarcasAltitud(ejeMinimo: Double, ejeMaximo: Double): List<Int> {
    for (paso in generarPasosRedondos(exponenteMinimo = 0, exponenteMaximo = 8)) {
        val primera = ceil(ejeMinimo / paso).toLong()
        val ultima = floor(ejeMaximo / paso).toLong()
        if (ultima - primera + 1 <= MAXIMO_MARCAS_ALTITUD) {
            return (primera..ultima).map { multiplo -> (multiplo * paso).roundToInt() }
        }
    }
    return emptyList()
}

// Generar los pasos 1, 2, 5, 10, 20, 50... en orden creciente
private fun generarPasosRedondos(exponenteMinimo: Int, exponenteMaximo: Int): Sequence<Double> {
    return (exponenteMinimo..exponenteMaximo).asSequence().flatMap { exponente ->
        MANTISAS_PASO_REDONDO.asSequence().map { mantisa -> mantisa * 10.0.pow(exponente) }
    }
}

// Elegir las marcas del eje x: múltiplos de un paso redondo desde cero, a lo sumo cinco
fun calcularMarcasKilometros(kilometrosTotales: Double): List<Double> {
    if (kilometrosTotales <= 0.0) return listOf(0.0)
    for (paso in generarPasosRedondos(exponenteMinimo = -3, exponenteMaximo = 6)) {
        val cantidad = floor(kilometrosTotales / paso + TOLERANCIA_DIVISION).toInt() + 1
        if (cantidad <= MAXIMO_MARCAS_KILOMETROS) {
            return (0 until cantidad).map { multiplo ->
                Math.round(multiplo * paso * FACTOR_REDONDEO_KILOMETROS) / FACTOR_REDONDEO_KILOMETROS
            }
        }
    }
    return listOf(0.0)
}

// Convertir la posición horizontal de un toque en kilómetros, limitada al área del gráfico
fun convertirPosicionAKilometros(posicionX: Float, izquierda: Float, derecha: Float, kilometrosTotales: Double): Double {
    if (derecha <= izquierda) return 0.0
    val fraccion = ((posicionX - izquierda) / (derecha - izquierda)).coerceIn(0f, 1f)
    return fraccion * kilometrosTotales
}

// Buscar el índice del punto del perfil más cercano a un kilómetro; nulo si no hay puntos
fun buscarIndicePuntoMasCercano(puntos: List<PuntoPerfil>, kilometros: Double): Int? {
    if (puntos.isEmpty()) return null
    return puntos.indices.minByOrNull { indice -> abs(puntos[indice].distanciaKilometros - kilometros) }
}

// Interpolar la altitud a un kilómetro entre las dos muestras vecinas; nulo si no hay puntos
fun interpolarAltitud(puntos: List<PuntoPerfil>, kilometros: Double): Double? {
    if (puntos.isEmpty()) return null
    return interpolarEntreMuestras(puntos.map { punto -> punto.distanciaKilometros }, puntos.map { punto -> punto.altitudMetros }, kilometros)
}

// Interpolar linealmente una serie de valores ordenada por kilómetro, limitada a los extremos
private fun interpolarEntreMuestras(kilometros: List<Double>, valores: List<Double>, kilometro: Double): Double {
    if (kilometro <= kilometros.first()) return valores.first()
    if (kilometro >= kilometros.last()) return valores.last()
    val siguiente = kilometros.indexOfFirst { posicion -> posicion > kilometro }
    val anterior = siguiente - 1
    val largoTramo = kilometros[siguiente] - kilometros[anterior]
    if (largoTramo <= 0.0) return valores[siguiente]
    val fraccion = (kilometro - kilometros[anterior]) / largoTramo
    return valores[anterior] + (valores[siguiente] - valores[anterior]) * fraccion
}

// Calcular el kilómetro recorrido a lo largo del trazado como largo total menos lo que falta
fun calcularKilometroEnTrazado(largoTotalKilometros: Double, distanciaRestanteKilometros: Double): Double {
    val largo = max(largoTotalKilometros, 0.0)
    return (largo - distanciaRestanteKilometros).coerceIn(0.0, largo)
}
