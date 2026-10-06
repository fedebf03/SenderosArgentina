package com.unaj.senderos.presentation.registro

data class RegistroEstadoVisual(
    val email: String = "",
    val clave: String = "",
    val confirmacion: String = "",
    val esClaveVisible: Boolean = false,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

sealed interface RegistroEvento {
    data class AlCambiarEmail(val email: String) : RegistroEvento
    data class AlCambiarClave(val clave: String) : RegistroEvento
    data class AlCambiarConfirmacion(val confirmacion: String) : RegistroEvento
    data object AlAlternarVisibilidadClave : RegistroEvento
    data object AlRegistrarse : RegistroEvento
}

sealed interface RegistroEfecto {
    data object NavegarAInicio : RegistroEfecto
}
