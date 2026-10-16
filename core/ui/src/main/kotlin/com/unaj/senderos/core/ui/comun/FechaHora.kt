package com.unaj.senderos.core.ui.comun

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", LOCALIDAD_ARGENTINA)

fun formatearFechaHora(milisegundos: Long, zona: ZoneId = ZoneId.systemDefault()): String {
    return FORMATO_FECHA_HORA.format(Instant.ofEpochMilli(milisegundos).atZone(zona))
}
