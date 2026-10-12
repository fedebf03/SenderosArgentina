package com.unaj.senderos.falsos

import com.unaj.senderos.domain.repository.AlmacenFotos

class FalsoAlmacenFotos : AlmacenFotos {

    val urisCopiadas: MutableList<String> = mutableListOf()
    val rutasBorradas: MutableList<String> = mutableListOf()
    var error: Throwable? = null

    // Registrar la Uri y devolver una ruta local determinística, o el error configurado
    override suspend fun guardarCopia(uriOrigen: String): Result<String> {
        urisCopiadas.add(uriOrigen)
        error?.let { falla -> return Result.failure(falla) }
        return Result.success("/fotos/copia-${urisCopiadas.size}.jpg")
    }

    // Registrar la ruta borrada
    override suspend fun borrar(ruta: String) {
        rutasBorradas.add(ruta)
    }
}
