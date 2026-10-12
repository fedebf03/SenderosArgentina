package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.CalculadoraRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.presentation.formulario.obtenerTextoTipoPunto
import com.unaj.senderos.presentation.mapa.tieneCoordenadasValidas
import java.util.Locale

private val LOCALIDAD_ARGENTINA = Locale.forLanguageTag("es-AR")

// Punto del recorrido que no es un simple paso, con su posición original y su distancia desde el inicio
data class PuntoDeInteres(
    val indice: Int,
    val tipo: TipoPunto,
    val nombre: String,
    val distanciaDesdeInicioKilometros: Double
) {
    // Armar el texto "Tipo · Nombre", o solo el tipo cuando el punto no tiene nombre
    fun texto(): String {
        val textoTipo = obtenerTextoTipoPunto(tipo)
        return if (nombre.isBlank()) textoTipo else "$textoTipo · ${nombre.trim()}"
    }
}

// Listar los puntos que no son de paso con su distancia acumulada a lo largo del trazado
internal fun obtenerPuntosDeInteres(sendero: Sendero?): List<PuntoDeInteres> {
    val puntosValidos = sendero?.recorrido.orEmpty().withIndex().filter { (_, punto) -> punto.tieneCoordenadasValidas() }
    val distancias = CalculadoraRecorrido.distanciasAcumuladasKilometros(puntosValidos.map { punto -> punto.value })
    return puntosValidos.mapIndexedNotNull { posicion, (indice, punto) ->
        if (punto.tipo == TipoPunto.PASO) {
            null
        } else {
            PuntoDeInteres(indice, punto.tipo, punto.nombre, distancias[posicion])
        }
    }
}

// Formatear la distancia desde el inicio como "km X,X" con el formato de Argentina
internal fun formatearKilometrosDesdeInicio(kilometros: Double): String {
    return "km " + String.format(LOCALIDAD_ARGENTINA, "%.1f", kilometros)
}

// Obtener la coordenada del punto del recorrido en la posición indicada, o nulo si no existe
internal fun DetalleSenderoEstadoVisual.coordenadaDelPunto(indice: Int): Coordenada? {
    val punto = sendero?.recorrido?.getOrNull(indice) ?: return null
    return Coordenada(punto.latitud, punto.longitud)
}
