package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.SenderoPublico
import kotlinx.coroutines.flow.Flow

interface SenderoPublicoRepositorio {
    fun observarSenderosPublicos(): Flow<List<SenderoPublico>>

    fun observarSenderoPublico(idSendero: String): Flow<SenderoPublico?>

    suspend fun publicar(sendero: SenderoPublico): Result<Unit>

    suspend fun despublicar(idSendero: String): Result<Unit>
}
