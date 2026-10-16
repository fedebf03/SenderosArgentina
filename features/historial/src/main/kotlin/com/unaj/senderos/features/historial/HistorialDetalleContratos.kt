package com.unaj.senderos.features.historial

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.model.Sendero

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
