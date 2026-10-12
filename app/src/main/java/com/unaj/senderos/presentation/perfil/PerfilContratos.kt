package com.unaj.senderos.presentation.perfil

import com.unaj.senderos.domain.model.Usuario

data class PerfilEstadoVisual(
    val usuario: Usuario? = null,
    val esModoDemostracion: Boolean = false
)

sealed interface PerfilEvento {
    data object AlCerrarSesion : PerfilEvento
    data class AlCambiarModoDemostracion(val activo: Boolean) : PerfilEvento
}

sealed interface PerfilEfecto {
    data object NavegarALogin : PerfilEfecto
}
