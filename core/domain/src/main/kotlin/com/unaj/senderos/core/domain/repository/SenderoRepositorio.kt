package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Sendero
import kotlinx.coroutines.flow.Flow

interface SenderoRepositorio {
    // Observar la lista de senderos del usuario desde la fuente local
    fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>>

    // Observar un sendero del usuario desde la fuente local
    fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?>

    // Invertir la marca de favorito de un sendero
    suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit>

    // Eliminar un sendero del usuario con borrado lógico
    suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit>

    // Guardar un sendero del usuario
    suspend fun guardarSendero(idUsuario: String, sendero: Sendero): Result<Unit>

    // Detener la sincronización pendiente del usuario
    fun detenerSincronizacion(idUsuario: String)
}
