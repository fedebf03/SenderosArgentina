package com.unaj.senderos.core.domain.model

// La foto es una ruta local o una URL remota, y queda vacía si no tiene
data class PerfilUsuario(
    val nombre: String = "",
    val pais: String = "",
    val provincia: String = "",
    val localidad: String = "",
    val foto: String = ""
) {
    val ubicacionTexto: String
        get() = listOf(localidad, provincia, pais).filter { texto -> texto.isNotBlank() }.joinToString(", ")
}

// El perfil queda siempre en el dispositivo. La nube puede esperar si no hubo conexión o falló la foto
data class ResultadoGuardadoPerfil(val estaEnLaNube: Boolean)
