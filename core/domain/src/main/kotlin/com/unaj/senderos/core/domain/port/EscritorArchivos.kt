package com.unaj.senderos.core.domain.port

interface EscritorArchivos {
    suspend fun escribirTexto(uri: String, texto: String): Result<Unit>
}
