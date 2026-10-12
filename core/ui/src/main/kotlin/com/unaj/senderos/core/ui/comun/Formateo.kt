package com.unaj.senderos.presentation.comun

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

// Localidad única con la que se dan formato a los números visibles (coma decimal, punto de miles)
internal val LOCALIDAD_ARGENTINA: Locale = Locale.forLanguageTag("es-AR")

// Texto que reemplaza a un valor que todavía no se conoce
const val TEXTO_SIN_DATO = "—"

// Texto que indica que el dispositivo no puede medir un valor
const val TEXTO_NO_DISPONIBLE = "No disponible en este dispositivo"

private const val KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO = 3.6f
private const val METROS_POR_KILOMETRO = 1_000
private const val KILOMETROS_PARA_OMITIR_DECIMAL = 10

// Dar formato a kilómetros con un decimal y coma, sin decimal cuando el valor redondeado es entero
fun formatearKilometros(kilometros: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f", kilometros).removeSuffix(",0")
}

// Dar formato "12,5 km" a la distancia de un sendero para tarjetas y datos del detalle
fun formatearDistanciaSendero(kilometros: Double): String {
    return "${formatearKilometros(kilometros)} km"
}

// Dar formato en metros (menos de 1 km) o en kilómetros con un decimal a una distancia corta
fun formatearDistanciaCorta(kilometros: Double): String {
    if (kilometros < 1.0) {
        return "${(kilometros * METROS_POR_KILOMETRO).roundToInt()} m"
    }
    return String.format(LOCALIDAD_ARGENTINA, "%.1f km", kilometros)
}

// Dar formato "a 850 m de vos": metros bajo 1 km, un decimal hasta 10 km y entero a partir de allí
fun formatearDistanciaAlUsuario(distanciaKilometros: Double): String {
    val metros = (distanciaKilometros * METROS_POR_KILOMETRO).roundToInt()
    if (metros < METROS_POR_KILOMETRO) return "a $metros m de vos"
    val kilometrosConDecimal = (distanciaKilometros * 10).roundToInt() / 10.0
    if (kilometrosConDecimal < KILOMETROS_PARA_OMITIR_DECIMAL) {
        return "a ${String.format(LOCALIDAD_ARGENTINA, "%.1f", kilometrosConDecimal)} km de vos"
    }
    return "a ${distanciaKilometros.roundToInt()} km de vos"
}

// Dar formato "7,2 km/h" a una velocidad en kilómetros por hora
fun formatearVelocidad(kilometrosPorHora: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f km/h", kilometrosPorHora)
}

// Dar formato "5,4 km/h" a una velocidad en metros por segundo, o "—" si no se conoce
fun formatearVelocidadDesdeMetrosPorSegundo(metrosPorSegundo: Float?): String {
    if (metrosPorSegundo == null) return TEXTO_SIN_DATO
    return formatearVelocidad((metrosPorSegundo * KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO).toDouble())
}

// Dar formato con separador de miles argentino a los pasos, o el texto indicado si no se conocen
fun formatearPasos(pasos: Int?, textoSinDato: String = TEXTO_SIN_DATO): String {
    return if (pasos == null) textoSinDato else String.format(LOCALIDAD_ARGENTINA, "%,d", pasos)
}

// Dar formato "95 pasos/min" al ritmo, o el texto indicado si no se conoce
fun formatearRitmo(pasosPorMinuto: Double?, textoSinDato: String = TEXTO_SIN_DATO): String {
    return if (pasosPorMinuto == null) textoSinDato else "${pasosPorMinuto.roundToInt()} pasos/min"
}

// Dar formato editable a un número para un campo de texto: coma decimal, sin separador de miles y sin decimal si es entero
fun formatearDecimalEditable(valor: Double): String {
    val formato = DecimalFormat("0.##########", DecimalFormatSymbols(LOCALIDAD_ARGENTINA))
    return formato.format(valor)
}

private const val BYTES_POR_KILOBYTE = 1024.0
private const val BYTES_POR_MEGABYTE = 1024.0 * 1024.0

// Dar formato con un decimal y coma decimal a un número, sin unidad
fun formatearUnDecimal(valor: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f", valor)
}

// Dar formato "12,5 MB" o "1,5 KB" con un decimal y coma decimal al tamaño de un mapa descargado
fun formatearTamanoMapa(bytes: Long): String {
    return if (bytes >= BYTES_POR_MEGABYTE) {
        "${formatearUnDecimal(bytes / BYTES_POR_MEGABYTE)} MB"
    } else {
        "${formatearUnDecimal(bytes / BYTES_POR_KILOBYTE)} KB"
    }
}
