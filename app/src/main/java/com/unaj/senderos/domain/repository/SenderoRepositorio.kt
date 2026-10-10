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

    // Crear o actualizar un sendero del usuario (escritura diferida: local primero, luego sincronización)
    suspend fun guardarSendero(idUsuario: String, sendero: Sendero): Result<Unit>

    // Detener la sincronización pendiente o en curso del usuario (por ejemplo al cerrar sesión)
    fun detenerSincronizacion(idUsuario: String)
}
