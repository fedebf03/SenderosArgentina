package com.unaj.senderos.features.perfil

private const val MILISEGUNDOS_POR_MINUTO = 60_000L
private const val MINUTOS_POR_HORA = 60L

// Expresar el tiempo total en horas y minutos, sin segundos sueltos: "45 min", "3 h" o "125 h 20 min"
fun describirTiempoTotal(milisegundos: Long): String {
    val minutosTotales = milisegundos / MILISEGUNDOS_POR_MINUTO
    val horas = minutosTotales / MINUTOS_POR_HORA
    val minutos = minutosTotales % MINUTOS_POR_HORA
    return when {
        horas == 0L -> "$minutos min"
        minutos == 0L -> "$horas h"
        else -> "$horas h $minutos min"
    }
}

// Aclarar cuántos recorridos de demostración quedaron fuera de las estadísticas; vacío si no hubo ninguno
fun describirSimuladosIgnorados(cantidad: Int): String = when (cantidad) {
    0 -> ""
    1 -> "No incluye 1 recorrido simulado"
    else -> "No incluye $cantidad recorridos simulados"
}
