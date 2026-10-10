package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.model.Sendero

enum class SeccionDetalle { RECOMENDACIONES, SERVICIOS, ETAPAS }

data class DetalleSenderoEstadoVisual(
    val sendero: Sendero? = null,
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val seccionesExpandidas: Set<SeccionDetalle> = emptySet(),
    val esMenuOpcionesVisible: Boolean = false,
    val esDialogoEliminarVisible: Boolean = false
)

sealed interface DetalleSenderoEvento {
    data object AlVolver : DetalleSenderoEvento
    data object AlAlternarFavorito : DetalleSenderoEvento
    data object AlCompartir : DetalleSenderoEvento
    data class AlAlternarSeccion(val seccion: SeccionDetalle) : DetalleSenderoEvento
    data object AlTocarIniciarRecorrido : DetalleSenderoEvento
    data object AlAbrirMenuOpciones : DetalleSenderoEvento
    data object AlCerrarMenuOpciones : DetalleSenderoEvento
    data object AlTocarEditar : DetalleSenderoEvento
    data object AlTocarEliminar : DetalleSenderoEvento
    data object AlCancelarEliminar : DetalleSenderoEvento
    data object AlConfirmarEliminar : DetalleSenderoEvento
}

sealed interface DetalleSenderoEfecto {
    data object VolverAlListado : DetalleSenderoEfecto
    data class MostrarAviso(val mensaje: String) : DetalleSenderoEfecto
    data class CompartirSendero(val texto: String) : DetalleSenderoEfecto
}
