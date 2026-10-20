package com.unaj.senderos.core.ui.clima

import java.time.LocalDate
import kotlin.math.roundToInt

private const val MILISEGUNDOS_POR_MINUTO = 60_000L
private const val MINUTOS_POR_HORA = 60L
private const val HORAS_POR_DIA = 24L
private const val VIENTO_CALMO_KILOMETROS_POR_HORA = 1.0
// Los nombres van fijos (no según el idioma del equipo) para que se vean igual en cualquier teléfono
private val NOMBRES_DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

fun formatearTemperaturaClima(celsius: Double): String = "${celsius.roundToInt()} °C"

fun describirVientoClima(kilometrosPorHora: Double, rafagasKilometrosPorHora: Double?): String {
    if (kilometrosPorHora < VIENTO_CALMO_KILOMETROS_POR_HORA) return "Calma"
    val viento = kilometrosPorHora.roundToInt()
    val rafagas = rafagasKilometrosPorHora?.roundToInt()
    return if (rafagas != null && rafagas > viento) "Viento $viento km/h, ráfagas $rafagas km/h" else "Viento $viento km/h"
}

fun describirAntiguedadClima(milisegundos: Long): String {
    val minutos = milisegundos / MILISEGUNDOS_POR_MINUTO
    return when {
        minutos < 1 -> "recién"
        minutos < MINUTOS_POR_HORA -> "hace $minutos min"
        minutos < MINUTOS_POR_HORA * HORAS_POR_DIA -> "hace ${minutos / MINUTOS_POR_HORA} h"
        else -> {
            val dias = minutos / (MINUTOS_POR_HORA * HORAS_POR_DIA)
            if (dias == 1L) "hace 1 día" else "hace $dias días"
        }
    }
}

fun describirDiaClima(fecha: String, hoy: String): String {
    val dia = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return fecha
    val fechaHoy = runCatching { LocalDate.parse(hoy) }.getOrNull()
    return when {
        fechaHoy != null && dia == fechaHoy -> "Hoy"
        fechaHoy != null && dia == fechaHoy.plusDays(1) -> "Mañana"
        else -> "${NOMBRES_DIAS[dia.dayOfWeek.value - 1]} ${dia.dayOfMonth}"
    }
}

fun describirProbabilidadLluvia(porcentaje: Int?): String = if (porcentaje == null) "" else "$porcentaje %"

fun describirAvisoPronosticoViejo(antiguedadMilisegundos: Long): String {
    if (antiguedadMilisegundos < MILISEGUNDOS_POR_MINUTO) return "Sin conexión: mostrando el último pronóstico guardado"
    return "Sin conexión: pronóstico guardado ${describirAntiguedadClima(antiguedadMilisegundos)}"
}
