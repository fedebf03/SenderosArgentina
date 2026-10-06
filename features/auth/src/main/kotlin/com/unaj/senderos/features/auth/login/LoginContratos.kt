package com.unaj.senderos.presentation.login

data class LoginEstadoVisual(
    val email: String = "",
    val clave: String = "",
    val esClaveVisible: Boolean = false,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

sealed interface LoginEvento {
    data class AlCambiarEmail(val email: String) : LoginEvento
    data class AlCambiarClave(val clave: String) : LoginEvento
    data object AlAlternarVisibilidadClave : LoginEvento
    data object AlIniciarSesion : LoginEvento
    data object AlOlvidarClave : LoginEvento
    data class AlContinuarConGoogle(val idToken: String) : LoginEvento
    data object AlSolicitarGoogleSignIn : LoginEvento
    data object AlIrARegistro : LoginEvento
}

sealed interface LoginEfecto {
    data object NavegarAInicio : LoginEfecto
    data object NavegarARegistro : LoginEfecto
    data class MostrarAviso(val mensaje: String) : LoginEfecto
    data object SolicitarGoogleSignIn : LoginEfecto
}
