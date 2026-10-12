package com.unaj.senderos.presentation.historial

import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.domain.model.Sendero

data class HistorialDetalleEstadoVisual(
    val recorrido: RecorridoRealizado? = null,
    val sendero: Sendero? = null,
    val estaCargando: Boolean = true,
    val mensajeError: String? = null
)

sealed interface HistorialDetalleEvento {
    data object AlVolver : HistorialDetalleEvento
}

sealed interface HistorialDetalleEfecto {
    data object VolverAlPerfil : HistorialDetalleEfecto
}
