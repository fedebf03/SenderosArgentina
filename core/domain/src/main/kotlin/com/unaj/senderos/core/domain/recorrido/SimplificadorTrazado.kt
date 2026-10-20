package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import kotlin.math.cos
import kotlin.math.hypot

private const val METROS_POR_GRADO_LATITUD = 110_540.0
private const val METROS_POR_GRADO_LONGITUD_EN_EL_ECUADOR = 111_320.0

object SimplificadorTrazado {

    // Usamos el algoritmo de Douglas-Peucker. Se conserva un punto si se aparta más de la tolerancia de la recta entre
    // los conservados vecinos
    fun simplificar(puntos: List<Coordenada>, toleranciaMetros: Double): List<Int> {
        if (puntos.size <= 2) return puntos.indices.toList()
        val latitudReferencia = Math.toRadians(puntos.first().latitud)
        val planos = puntos.map { punto -> proyectar(punto, latitudReferencia) }
        val conservados = BooleanArray(puntos.size)
        conservados[0] = true
        conservados[puntos.lastIndex] = true
        // Se usa una pila en lugar de recursión para no desbordarla con trazados de decenas de miles de puntos
        val pendientes = ArrayDeque<Pair<Int, Int>>()
        pendientes.addLast(0 to puntos.lastIndex)
        while (pendientes.isNotEmpty()) {
            val (desde, hasta) = pendientes.removeLast()
            var indiceMasLejano = -1
            var distanciaMasLejana = 0.0
            for (indice in desde + 1 until hasta) {
                val distancia = distanciaASegmento(planos[indice], planos[desde], planos[hasta])
                if (distancia > distanciaMasLejana) {
                    distanciaMasLejana = distancia
                    indiceMasLejano = indice
                }
            }
            if (indiceMasLejano >= 0 && distanciaMasLejana > toleranciaMetros) {
                conservados[indiceMasLejano] = true
                pendientes.addLast(desde to indiceMasLejano)
                pendientes.addLast(indiceMasLejano to hasta)
            }
        }
        return conservados.indices.filter { indice -> conservados[indice] }
    }

    // Un plano local alcanza para las distancias de un sendero
    internal fun proyectar(punto: Coordenada, latitudReferenciaRadianes: Double): Pair<Double, Double> =
        punto.longitud * METROS_POR_GRADO_LONGITUD_EN_EL_ECUADOR * cos(latitudReferenciaRadianes) to
            punto.latitud * METROS_POR_GRADO_LATITUD

    internal fun distanciaASegmento(punto: Pair<Double, Double>, inicio: Pair<Double, Double>, fin: Pair<Double, Double>): Double {
        val (x, y) = punto
        val (x1, y1) = inicio
        val (x2, y2) = fin
        val dx = x2 - x1
        val dy = y2 - y1
        val largoCuadrado = dx * dx + dy * dy
        if (largoCuadrado == 0.0) return hypot(x - x1, y - y1)
        val fraccion = (((x - x1) * dx + (y - y1) * dy) / largoCuadrado).coerceIn(0.0, 1.0)
        return hypot(x - (x1 + fraccion * dx), y - (y1 + fraccion * dy))
    }
}
