package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Sendero
import kotlinx.coroutines.flow.Flow

interface SenderoRepositorio {
    // Observar los senderos que pertenecen al usuario indicado
    fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>>

    // Observar un sendero del usuario; emite null si no existe o fue eliminado
    fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?>

    // Alternar la marca de favorito de un sendero del usuario
    suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit>

    // Eliminar un sendero del usuario
    suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit>
}
