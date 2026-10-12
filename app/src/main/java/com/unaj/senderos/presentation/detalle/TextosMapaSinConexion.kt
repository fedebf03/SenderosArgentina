package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.presentation.comun.formatearTamanoMapa
import com.unaj.senderos.domain.model.EstadoMapaSinConexion


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
