package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val RADIO_TIERRA_KILOMETROS = 6371.0

object CalculadoraRecorrido {

    // Calcular la distancia total del recorrido en kilómetros según su tipo
    fun distanciaKilometros(puntos: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): Double {
        if (puntos.size < 2) return 0.0
        var total = puntos.zipWithNext().sumOf { (origen, destino) -> distanciaEntre(origen, destino) }
        if (tipoRecorrido == TipoRecorrido.CIRCULAR) {
            total += distanciaEntre(puntos.last(), puntos.first())
        }
        if (tipoRecorrido == TipoRecorrido.IDA_Y_VUELTA) {
            total *= 2
        }
        return total
    }

    // Calcular la distancia acumulada en kilómetros desde el inicio hasta cada punto, siguiendo el orden del recorrido
    fun distanciasAcumuladasKilometros(puntos: List<PuntoRecorrido>): List<Double> {
        var acumulada = 0.0
        return puntos.mapIndexed { indice, punto ->
            if (indice > 0) acumulada += distanciaEntre(puntos[indice - 1], punto)
            acumulada
        }
    }

    // Calcular la distancia en kilómetros desde una coordenada hasta un punto del recorrido
    fun distanciaEntre(desde: Coordenada, hasta: PuntoRecorrido): Double {
        return haversine(desde.latitud, desde.longitud, hasta.latitud, hasta.longitud)
    }

    // Calcular la distancia en kilómetros entre dos puntos consecutivos del recorrido
    private fun distanciaEntre(origen: PuntoRecorrido, destino: PuntoRecorrido): Double {
        return haversine(origen.latitud, origen.longitud, destino.latitud, destino.longitud)
    }

    // Calcular la distancia en kilómetros entre dos pares de coordenadas con la fórmula de Haversine
    private fun haversine(latitudOrigen: Double, longitudOrigen: Double, latitudDestino: Double, longitudDestino: Double): Double {
        val diferenciaLatitud = Math.toRadians(latitudDestino - latitudOrigen)
        val diferenciaLongitud = Math.toRadians(longitudDestino - longitudOrigen)
        val parcial = sin(diferenciaLatitud / 2).pow(2) +
            cos(Math.toRadians(latitudOrigen)) * cos(Math.toRadians(latitudDestino)) *
            sin(diferenciaLongitud / 2).pow(2)
        return 2 * RADIO_TIERRA_KILOMETROS * asin(sqrt(parcial))
    }

    // Calcular el rumbo inicial en grados [0, 360) desde una coordenada hacia otra
    fun rumboInicialGrados(desde: Coordenada, hasta: Coordenada): Double {
        val latitudDesde = Math.toRadians(desde.latitud)
        val latitudHasta = Math.toRadians(hasta.latitud)
        val diferenciaLongitud = Math.toRadians(hasta.longitud - desde.longitud)
        val este = sin(diferenciaLongitud) * cos(latitudHasta)
        val norte = cos(latitudDesde) * sin(latitudHasta) - sin(latitudDesde) * cos(latitudHasta) * cos(diferenciaLongitud)
        return (Math.toDegrees(atan2(este, norte)) + 360.0) % 360.0
    }

    // Normalizar un rumbo cualquiera al intervalo [0, 360)
    fun normalizarRumbo(grados: Float): Float = ((grados % 360f) + 360f) % 360f

    // Invertir el sentido del recorrido devolviendo los puntos en orden contrario
    fun invertirSentido(puntos: List<PuntoRecorrido>): List<PuntoRecorrido> {
        return puntos.reversed()
    }
}
