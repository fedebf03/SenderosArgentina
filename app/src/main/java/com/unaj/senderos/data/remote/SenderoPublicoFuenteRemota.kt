package com.unaj.senderos.data.remote

import kotlinx.coroutines.flow.Flow

interface SenderoPublicoFuenteRemota {
    fun observarTodos(): Flow<List<SenderoPublicoRed>>

    fun observarUno(idSendero: String): Flow<SenderoPublicoRed?>

    suspend fun guardar(sendero: SenderoPublicoRed)

    suspend fun eliminar(idSendero: String)
}
