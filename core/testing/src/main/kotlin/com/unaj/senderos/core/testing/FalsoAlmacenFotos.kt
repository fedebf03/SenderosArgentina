package com.unaj.senderos.falsos

import com.unaj.senderos.domain.port.AlmacenFotos

class FalsoAlmacenFotos : AlmacenFotos {

    val urisCopiadas: MutableList<String> = mutableListOf()
    val rutasBorradas: MutableList<String> = mutableListOf()
    var error: Throwable? = null

    override suspend fun guardarCopia(uriOrigen: String): Result<String> {
        urisCopiadas.add(uriOrigen)
        error?.let { falla -> return Result.failure(falla) }
        return Result.success("/fotos/copia-${urisCopiadas.size}.jpg")
    }

    override suspend fun borrar(ruta: String) {
        rutasBorradas.add(ruta)
    }
}
