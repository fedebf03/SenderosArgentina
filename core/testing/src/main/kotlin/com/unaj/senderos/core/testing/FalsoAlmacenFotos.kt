package com.unaj.senderos.falsos

import com.unaj.senderos.domain.repository.AlmacenFotos

class FalsoAlmacenFotos : AlmacenFotos {

    var errorAlGuardar: Throwable? = null
    val urisRecibidas: MutableList<String> = mutableListOf()

    override suspend fun guardarCopia(uriOrigen: String): Result<String> {
        urisRecibidas.add(uriOrigen)
        errorAlGuardar?.let { error -> return Result.failure(error) }
        return Result.success("/data/fotos_senderos/copia-${urisRecibidas.size}.jpg")
    }
}
