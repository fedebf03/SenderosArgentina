package com.unaj.senderos.features.historial

import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.recorrido.ConversorCaminata
import com.unaj.senderos.core.domain.model.Sendero

data class HistorialDetalleEstadoVisual(
    val recorrido: RecorridoRealizado? = null,
    val sendero: Sendero? = null,
    val estaCargando: Boolean = true,
    val mensajeError: String? = null
) {
    // Una caminata libre tiene identificador propio en lugar del de un sendero
    val esCaminataLibre: Boolean
        get() = recorrido != null && IdentificadorCaminataLibre.esDeCaminataLibre(recorrido.idSendero)

    // Solo una caminata libre de al menos 50 m se puede convertir en un sendero nuevo
    val sePuedeConvertirEnSendero: Boolean
        get() = esCaminataLibre && recorrido != null && ConversorCaminata.esConvertible(recorrido.distanciaKilometros)
}

sealed interface HistorialDetalleEvento {
    data object AlVolver : HistorialDetalleEvento
    data object AlConvertirEnSendero : HistorialDetalleEvento
}

sealed interface HistorialDetalleEfecto {
    data object VolverAlPerfil : HistorialDetalleEfecto
    data class NavegarAFormularioDesdeCaminata(val idCaminata: String) : HistorialDetalleEfecto
}
