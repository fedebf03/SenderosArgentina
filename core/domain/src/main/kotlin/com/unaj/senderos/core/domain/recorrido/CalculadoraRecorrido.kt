package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido
import kotlin.math.asin
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

    private fun distanciaEntre(origen: PuntoRecorrido, destino: PuntoRecorrido): Double {
        val diferenciaLatitud = Math.toRadians(destino.latitud - origen.latitud)
        val diferenciaLongitud = Math.toRadians(destino.longitud - origen.longitud)
        val parcial = sin(diferenciaLatitud / 2).pow(2) +
            cos(Math.toRadians(origen.latitud)) * cos(Math.toRadians(destino.latitud)) *
            sin(diferenciaLongitud / 2).pow(2)
        return 2 * RADIO_TIERRA_KILOMETROS * asin(sqrt(parcial))
    }

    fun invertirSentido(puntos: List<PuntoRecorrido>): List<PuntoRecorrido> {
        return puntos.reversed()
    }
}
