package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Sendero
import kotlinx.coroutines.flow.Flow

interface SenderoRepositorio {
    fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>>

    fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?>

    suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit>

    // Borrado lógico
    suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit>

    suspend fun guardarSendero(idUsuario: String, sendero: Sendero): Result<Unit>

    fun detenerSincronizacion(idUsuario: String)
}
