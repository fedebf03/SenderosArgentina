package com.unaj.senderos.core.domain.model

// Datos del perfil que el usuario puede editar; la foto es una ruta local o una URL remota, vacía si no tiene
data class PerfilUsuario(
    val nombre: String = "",
    val pais: String = "",
    val provincia: String = "",
    val localidad: String = "",
    val foto: String = ""
) {
    // Describir de dónde es el usuario con los datos que haya cargado, de lo más chico a lo más grande
    val ubicacionTexto: String
        get() = listOf(localidad, provincia, pais).filter { texto -> texto.isNotBlank() }.joinToString(", ")
}

// Resultado de guardar el perfil: queda en el dispositivo siempre; la nube puede esperar si no hubo conexión o falló la foto
data class ResultadoGuardadoPerfil(val estaEnLaNube: Boolean)
