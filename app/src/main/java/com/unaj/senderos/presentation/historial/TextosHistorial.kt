package com.unaj.senderos.presentation.historial

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val LOCALIDAD_ARGENTINA = Locale.forLanguageTag("es-AR")
private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", LOCALIDAD_ARGENTINA)

const val TEXTO_NO_DISPONIBLE = "No disponible en este dispositivo"

// Dar formato "12/10/2026 · 01:33" a un instante en milisegundos según la zona indicada
fun formatearFechaHora(milisegundos: Long, zona: ZoneId = ZoneId.systemDefault()): String {
    return FORMATO_FECHA_HORA.format(Instant.ofEpochMilli(milisegundos).atZone(zona))
}

// Dar formato "7,2 km/h" a una velocidad en kilómetros por hora
fun formatearVelocidad(kilometrosPorHora: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f km/h", kilometrosPorHora)
}

// Dar formato a la cantidad de pasos, o indicar que no está disponible si es nula
fun formatearPasos(pasos: Int?): String {
    return if (pasos == null) TEXTO_NO_DISPONIBLE else String.format(LOCALIDAD_ARGENTINA, "%,d", pasos)
}

// Dar formato "95 pasos/min" al ritmo, o indicar que no está disponible si es nulo
fun formatearRitmo(pasosPorMinuto: Double?): String {
    return if (pasosPorMinuto == null) TEXTO_NO_DISPONIBLE else "${pasosPorMinuto.roundToInt()} pasos/min"
}
