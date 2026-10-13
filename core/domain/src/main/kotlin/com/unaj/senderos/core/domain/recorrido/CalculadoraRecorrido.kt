package com.unaj.senderos.domain.recorrido

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

    fun distanciasAcumuladasKilometros(puntos: List<PuntoRecorrido>): List<Double> {
        var acumulada = 0.0
        return puntos.mapIndexed { indice, punto ->
            if (indice > 0) acumulada += distanciaEntre(puntos[indice - 1], punto)
            acumulada
        }
    }

    fun distanciaEntre(desde: Coordenada, hasta: PuntoRecorrido): Double {
        return haversine(desde.latitud, desde.longitud, hasta.latitud, hasta.longitud)
    }

    private fun distanciaEntre(origen: PuntoRecorrido, destino: PuntoRecorrido): Double {
        return haversine(origen.latitud, origen.longitud, destino.latitud, destino.longitud)
    }

    private fun haversine(latitudOrigen: Double, longitudOrigen: Double, latitudDestino: Double, longitudDestino: Double): Double {
        val diferenciaLatitud = Math.toRadians(latitudDestino - latitudOrigen)
        val diferenciaLongitud = Math.toRadians(longitudDestino - longitudOrigen)
        val parcial = sin(diferenciaLatitud / 2).pow(2) +
            cos(Math.toRadians(latitudOrigen)) * cos(Math.toRadians(latitudDestino)) *
            sin(diferenciaLongitud / 2).pow(2)
        return 2 * RADIO_TIERRA_KILOMETROS * asin(sqrt(parcial))
    }

    fun rumboInicialGrados(desde: Coordenada, hasta: Coordenada): Double {
        val latitudDesde = Math.toRadians(desde.latitud)
        val latitudHasta = Math.toRadians(hasta.latitud)
        val diferenciaLongitud = Math.toRadians(hasta.longitud - desde.longitud)
        val este = sin(diferenciaLongitud) * cos(latitudHasta)
        val norte = cos(latitudDesde) * sin(latitudHasta) - sin(latitudDesde) * cos(latitudHasta) * cos(diferenciaLongitud)
        return (Math.toDegrees(atan2(este, norte)) + 360.0) % 360.0
    }

    fun normalizarRumbo(grados: Float): Float = ((grados % 360f) + 360f) % 360f

    fun invertirSentido(puntos: List<PuntoRecorrido>): List<PuntoRecorrido> {
        return puntos.reversed()
    }
}
