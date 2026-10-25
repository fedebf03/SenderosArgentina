package com.unaj.senderos.core.network.perfil

interface PerfilFuenteRemota {
    suspend fun obtenerPerfil(idUsuario: String): PerfilRed?

    suspend fun guardarPerfil(idUsuario: String, perfil: PerfilRed)
}
