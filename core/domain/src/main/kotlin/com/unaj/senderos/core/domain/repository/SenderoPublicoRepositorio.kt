package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.SenderoPublico
import kotlinx.coroutines.flow.Flow

interface SenderoPublicoRepositorio {
    fun observarSenderosPublicos(): Flow<List<SenderoPublico>>

    fun observarSenderoPublico(idSendero: String): Flow<SenderoPublico?>

    suspend fun publicar(sendero: SenderoPublico): Result<Unit>

    suspend fun despublicar(idSendero: String): Result<Unit>
}
