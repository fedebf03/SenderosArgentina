package com.unaj.senderos.presentation.comun

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

// Localidad única con la que se dan formato a los números visibles (coma decimal, punto de miles)
internal val LOCALIDAD_ARGENTINA: Locale = Locale.forLanguageTag("es-AR")

const val TEXTO_SIN_DATO = "—"

const val TEXTO_NO_DISPONIBLE = "No disponible en este dispositivo"

private const val KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO = 3.6f
private const val METROS_POR_KILOMETRO = 1_000
private const val KILOMETROS_PARA_OMITIR_DECIMAL = 10

fun formatearKilometros(kilometros: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f", kilometros).removeSuffix(",0")
}

fun formatearDistanciaSendero(kilometros: Double): String {
    return "${formatearKilometros(kilometros)} km"
}

fun formatearDistanciaCorta(kilometros: Double): String {
    if (kilometros < 1.0) {
        return "${(kilometros * METROS_POR_KILOMETRO).roundToInt()} m"
    }
    return String.format(LOCALIDAD_ARGENTINA, "%.1f km", kilometros)
}

fun formatearDistanciaAlUsuario(distanciaKilometros: Double): String {
    val metros = (distanciaKilometros * METROS_POR_KILOMETRO).roundToInt()
    if (metros < METROS_POR_KILOMETRO) return "a $metros m de vos"
    val kilometrosConDecimal = (distanciaKilometros * 10).roundToInt() / 10.0
    if (kilometrosConDecimal < KILOMETROS_PARA_OMITIR_DECIMAL) {
        return "a ${String.format(LOCALIDAD_ARGENTINA, "%.1f", kilometrosConDecimal)} km de vos"
    }
    return "a ${distanciaKilometros.roundToInt()} km de vos"
}

fun formatearVelocidad(kilometrosPorHora: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f km/h", kilometrosPorHora)
}

fun formatearVelocidadDesdeMetrosPorSegundo(metrosPorSegundo: Float?): String {
    if (metrosPorSegundo == null) return TEXTO_SIN_DATO
    return formatearVelocidad((metrosPorSegundo * KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO).toDouble())
}

fun formatearPasos(pasos: Int?, textoSinDato: String = TEXTO_SIN_DATO): String {
    return if (pasos == null) textoSinDato else String.format(LOCALIDAD_ARGENTINA, "%,d", pasos)
}

fun formatearRitmo(pasosPorMinuto: Double?, textoSinDato: String = TEXTO_SIN_DATO): String {
    return if (pasosPorMinuto == null) textoSinDato else "${pasosPorMinuto.roundToInt()} pasos/min"
}

fun formatearDecimalEditable(valor: Double): String {
    val formato = DecimalFormat("0.##########", DecimalFormatSymbols(LOCALIDAD_ARGENTINA))
    return formato.format(valor)
}

private const val BYTES_POR_KILOBYTE = 1024.0
private const val BYTES_POR_MEGABYTE = 1024.0 * 1024.0

fun formatearUnDecimal(valor: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f", valor)
}

fun formatearTamanoMapa(bytes: Long): String {
    return if (bytes >= BYTES_POR_MEGABYTE) {
        "${formatearUnDecimal(bytes / BYTES_POR_MEGABYTE)} MB"
    } else {
        "${formatearUnDecimal(bytes / BYTES_POR_KILOBYTE)} KB"
    }
}
