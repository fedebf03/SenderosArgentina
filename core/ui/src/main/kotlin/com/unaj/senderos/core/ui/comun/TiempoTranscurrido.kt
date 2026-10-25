package com.unaj.senderos.core.ui.comun

import java.util.Locale

private const val MILISEGUNDOS_POR_SEGUNDO = 1_000L

private const val SEGUNDOS_POR_MINUTO = 60L

private const val SEGUNDOS_POR_HORA = 3_600L

fun formatearTiempoTranscurrido(milisegundos: Long): String {
    val segundosTotales = (milisegundos.coerceAtLeast(0L)) / MILISEGUNDOS_POR_SEGUNDO
    val horas = segundosTotales / SEGUNDOS_POR_HORA
    val minutos = (segundosTotales % SEGUNDOS_POR_HORA) / SEGUNDOS_POR_MINUTO
    val segundos = segundosTotales % SEGUNDOS_POR_MINUTO
    return if (horas > 0) {
        String.format(Locale.US, "%d:%02d:%02d", horas, minutos, segundos)
    } else {
        String.format(Locale.US, "%02d:%02d", minutos, segundos)
    }
}
