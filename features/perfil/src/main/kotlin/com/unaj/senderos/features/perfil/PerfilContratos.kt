package com.unaj.senderos.features.perfil

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.model.Usuario

data class PerfilEstadoVisual(
    val usuario: Usuario? = null,
    val esModoDemostracion: Boolean = false,
    val recorridos: List<RecorridoRealizado> = emptyList(),
    val mensajeError: String? = null,
    val hayRecorridoEnCurso: Boolean = false,
    val esDialogoCerrarSesionVisible: Boolean = false
)

sealed interface PerfilEvento {
    data object AlTocarCerrarSesion : PerfilEvento
    data object AlConfirmarCerrarSesion : PerfilEvento
    data object AlCancelarCerrarSesion : PerfilEvento
    data class AlCambiarModoDemostracion(val activo: Boolean) : PerfilEvento
    data class AlSeleccionarRecorrido(val idRecorrido: String) : PerfilEvento
}

sealed interface PerfilEfecto {
    data object NavegarALogin : PerfilEfecto
    data class NavegarAHistorialDetalle(val idRecorrido: String) : PerfilEfecto
}
