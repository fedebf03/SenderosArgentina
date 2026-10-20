package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.PerfilElevacion
import kotlinx.coroutines.flow.Flow

interface SenderoRepositorio {
    fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>>

    fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?>

    suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit>

    // Borrado lógico
    suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit>

    suspend fun guardarSendero(idUsuario: String, sendero: Sendero): Result<Unit>

    // El perfil es un dato derivado, por eso no renueva la fecha de modificación. Falla si el trazado cambió
    suspend fun guardarPerfilElevacion(idUsuario: String, idSendero: String, perfil: PerfilElevacion): Result<Unit>

    fun detenerSincronizacion(idUsuario: String)
}
