package com.unaj.senderos.core.network

interface PerfilFuenteRemota {
    // Leer el perfil del servidor; nulo si el usuario todavía no guardó uno
    suspend fun obtenerPerfil(idUsuario: String): PerfilRed?

    suspend fun guardarPerfil(idUsuario: String, perfil: PerfilRed)
}
