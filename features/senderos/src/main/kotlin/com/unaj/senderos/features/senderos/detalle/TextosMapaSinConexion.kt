package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import java.util.Locale

private val LOCALIDAD_ARGENTINA = Locale.forLanguageTag("es-AR")
private const val BYTES_POR_KILOBYTE = 1024.0
private const val BYTES_POR_MEGABYTE = 1024.0 * 1024.0

// Dar formato "12,5 MB" o "1,5 KB" con un decimal y coma decimal al tamaño de un mapa descargado
fun formatearTamanoMapa(bytes: Long): String {
    return if (bytes >= BYTES_POR_MEGABYTE) {
        String.format(LOCALIDAD_ARGENTINA, "%.1f MB", bytes / BYTES_POR_MEGABYTE)
    } else {
        String.format(LOCALIDAD_ARGENTINA, "%.1f KB", bytes / BYTES_POR_KILOBYTE)
    }
}

// Redactar el progreso de una descarga activa con su porcentaje y el tamaño ya descargado
fun describirDescarga(estado: EstadoMapaSinConexion): String {
    return when (estado) {
        is EstadoMapaSinConexion.Descargando ->
            "Descargando… ${estado.porcentaje} % · ${formatearTamanoMapa(estado.bytesCompletados)}"
        is EstadoMapaSinConexion.EsperandoConexion ->
            "Esperando conexión… ${estado.porcentaje} % · ${formatearTamanoMapa(estado.bytesCompletados)}"
        else -> ""
    }
}
