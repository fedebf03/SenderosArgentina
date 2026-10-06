package com.unaj.senderos.presentation.explorar

import com.unaj.senderos.domain.model.Usuario

data class ExplorarEstadoVisual(
    val usuario: Usuario? = null,
    val estaCargando: Boolean = false,
    val tabSeleccionado: Int = 0
)

sealed interface ExplorarEvento {
    data object AlCerrarSesion : ExplorarEvento
    data class AlSeleccionarTab(val indice: Int) : ExplorarEvento
}

sealed interface ExplorarEfecto {
    data object NavegarALogin : ExplorarEfecto
    data class MostrarMensaje(val mensaje: String) : ExplorarEfecto
}
