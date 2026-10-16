package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.ui.comun.formatearTamanoMapa
import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion


fun describirDescarga(estado: EstadoMapaSinConexion): String {
    return when (estado) {
        is EstadoMapaSinConexion.Descargando ->
            "Descargando… ${estado.porcentaje} % · ${formatearTamanoMapa(estado.bytesCompletados)}"
        is EstadoMapaSinConexion.EsperandoConexion ->
            "Esperando conexión… ${estado.porcentaje} % · ${formatearTamanoMapa(estado.bytesCompletados)}"
        else -> ""
    }
}
