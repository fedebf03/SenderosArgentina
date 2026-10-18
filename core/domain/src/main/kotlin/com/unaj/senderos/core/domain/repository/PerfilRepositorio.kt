package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.model.ResultadoGuardadoPerfil

interface PerfilRepositorio {

    // Entregar el perfil guardado en el dispositivo; si todavía no hay, intenta bajarlo de la nube. Nulo si no existe en ningún lado
    suspend fun obtener(idUsuario: String): PerfilUsuario?

    // Guardar en el dispositivo y, si se puede, en la nube. Falla solo si no se pudo guardar localmente
    suspend fun guardar(idUsuario: String, perfil: PerfilUsuario): Result<ResultadoGuardadoPerfil>

    // Volver a mandar a la nube un perfil que quedó pendiente; no hace nada si no hay nada pendiente
    suspend fun sincronizarPendiente(idUsuario: String)
}
