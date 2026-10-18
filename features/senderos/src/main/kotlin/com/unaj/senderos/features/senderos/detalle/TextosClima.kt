package com.unaj.senderos.features.senderos.detalle

import java.time.LocalDate
import kotlin.math.roundToInt

private const val MILISEGUNDOS_POR_MINUTO = 60_000L
private const val MINUTOS_POR_HORA = 60L
private const val HORAS_POR_DIA = 24L
private const val VIENTO_CALMO_KILOMETROS_POR_HORA = 1.0
// Los nombres van fijos y no del idioma del equipo para que la pantalla se vea igual en cualquier teléfono
private val NOMBRES_DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

// Formatear la temperatura en grados enteros con la unidad
fun formatearTemperaturaClima(celsius: Double): String = "${celsius.roundToInt()} °C"

// Describir el viento y, si superan al viento sostenido, sus ráfagas; por debajo de 1 km/h es calma
fun describirVientoClima(kilometrosPorHora: Double, rafagasKilometrosPorHora: Double?): String {
    if (kilometrosPorHora < VIENTO_CALMO_KILOMETROS_POR_HORA) return "Calma"
    val viento = kilometrosPorHora.roundToInt()
    val rafagas = rafagasKilometrosPorHora?.roundToInt()
    return if (rafagas != null && rafagas > viento) "Viento $viento km/h, ráfagas $rafagas km/h" else "Viento $viento km/h"
}

// Expresar qué tan viejo es el pronóstico en minutos, horas o días, redondeando hacia abajo
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

// Nombrar un día del pronóstico: Hoy, Mañana o su día de la semana con el número; una fecha ilegible se muestra tal cual
fun describirDiaClima(fecha: String, hoy: String): String {
    val dia = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return fecha
    val fechaHoy = runCatching { LocalDate.parse(hoy) }.getOrNull()
    return when {
        fechaHoy != null && dia == fechaHoy -> "Hoy"
        fechaHoy != null && dia == fechaHoy.plusDays(1) -> "Mañana"
        else -> "${NOMBRES_DIAS[dia.dayOfWeek.value - 1]} ${dia.dayOfMonth}"
    }
}

// Mostrar la probabilidad de lluvia como porcentaje; vacía si el servicio no la informó
fun describirProbabilidadLluvia(porcentaje: Int?): String = if (porcentaje == null) "" else "$porcentaje %"

// Avisar que el pronóstico mostrado es una copia vieja porque no hubo conexión para renovarlo
fun describirAvisoPronosticoViejo(antiguedadMilisegundos: Long): String =
    "Sin conexión: pronóstico guardado ${describirAntiguedadClima(antiguedadMilisegundos)}"
