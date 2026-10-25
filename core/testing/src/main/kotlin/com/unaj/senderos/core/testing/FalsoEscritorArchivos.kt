package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.EscritorArchivos

class FalsoEscritorArchivos : EscritorArchivos {

    val escritos: MutableMap<String, String> = mutableMapOf()
    var fallo: Throwable? = null

    override suspend fun escribirTexto(uri: String, texto: String): Result<Unit> {
        fallo?.let { error -> return Result.failure(error) }
        escritos[uri] = texto
        return Result.success(Unit)
    }
}
