package com.unaj.senderos.core.ui.sendero

import com.unaj.senderos.core.ui.comun.formatearKilometros
import com.unaj.senderos.core.ui.comun.formatearUnDecimal
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.ui.componentes.mapa.tieneCoordenadasValidas

data class PuntoDeInteres(
    val indice: Int,
    val tipo: TipoPunto,
    val nombre: String,
    val distanciaDesdeInicioKilometros: Double,
    val foto: String = ""
) {
    fun descripcionAccesible(): String {
        val partes = listOfNotNull(
            obtenerTextoTipoPunto(tipo),
            nombre.trim().ifBlank { null },
            "kilómetro ${formatearKilometros(distanciaDesdeInicioKilometros)} desde el inicio"
        )
        return partes.joinToString(", ")
    }

    fun texto(): String {
        val textoTipo = obtenerTextoTipoPunto(tipo)
        return if (nombre.isBlank()) textoTipo else "$textoTipo · ${nombre.trim()}"
    }
}

fun obtenerPuntosDeInteres(sendero: Sendero?): List<PuntoDeInteres> {
    val puntosValidos = sendero?.recorrido.orEmpty().withIndex().filter { (_, punto) -> punto.tieneCoordenadasValidas() }
    val distancias = CalculadoraRecorrido.distanciasAcumuladasKilometros(puntosValidos.map { punto -> punto.value })
    return puntosValidos.mapIndexedNotNull { posicion, (indice, punto) ->
        if (punto.tipo == TipoPunto.PASO) {
            null
        } else {
            PuntoDeInteres(indice, punto.tipo, punto.nombre, distancias[posicion], punto.foto)
        }
    }
}

fun formatearKilometrosDesdeInicio(kilometros: Double): String {
    return "km " + formatearUnDecimal(kilometros)
}
