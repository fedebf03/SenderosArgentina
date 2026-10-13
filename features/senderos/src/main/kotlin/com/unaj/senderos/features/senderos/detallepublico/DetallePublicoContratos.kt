package com.unaj.senderos.presentation.detallepublico

import com.unaj.senderos.domain.model.SenderoPublico

data class DetallePublicoEstadoVisual(
    val senderoPublico: SenderoPublico? = null,
    val esPropio: Boolean = false,
    val idCopia: String? = null,
    val estaCargando: Boolean = true,
    val estaGuardando: Boolean = false,
    val mensajeError: String? = null
)

sealed interface DetallePublicoEvento {
    data object AlVolver : DetallePublicoEvento
    data object AlGuardarEnMisSenderos : DetallePublicoEvento
    data object AlVerMiCopia : DetallePublicoEvento
}

sealed interface DetallePublicoEfecto {
    data object VolverAExplorar : DetallePublicoEfecto
    data class MostrarAviso(val mensaje: String) : DetallePublicoEfecto
    data class NavegarAMiSendero(val idSendero: String) : DetallePublicoEfecto
}
