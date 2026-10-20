package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.model.ResultadoGuardadoPerfil

interface PerfilRepositorio {

    // Si todavía no hay perfil en el dispositivo, se intenta bajarlo de la nube
    suspend fun obtener(idUsuario: String): PerfilUsuario?

    // Se guarda en el dispositivo y, si se puede, en la nube. Falla solo si no se pudo guardar localmente
    suspend fun guardar(idUsuario: String, perfil: PerfilUsuario): Result<ResultadoGuardadoPerfil>

    suspend fun sincronizarPendiente(idUsuario: String)
}
