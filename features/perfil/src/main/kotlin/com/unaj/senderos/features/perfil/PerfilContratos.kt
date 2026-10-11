package com.unaj.senderos.presentation.perfil

import com.unaj.senderos.domain.model.Usuario

data class PerfilEstadoVisual(
    val usuario: Usuario? = null
)

sealed interface PerfilEvento {
    data object AlCerrarSesion : PerfilEvento
}

sealed interface PerfilEfecto {
    data object NavegarALogin : PerfilEfecto
}
