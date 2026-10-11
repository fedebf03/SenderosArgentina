package com.unaj.senderos.falsos

import com.unaj.senderos.data.remote.SubidorFotos

class FalsoSubidorFotos : SubidorFotos {

    var errorAlSubir: Throwable? = null
    val rutasSubidas: MutableList<String> = mutableListOf()

    override suspend fun subir(rutaLocal: String): String {
        errorAlSubir?.let { error -> throw error }
        rutasSubidas.add(rutaLocal)
        return "https://res.cloudinary.com/prueba/${rutaLocal.substringAfterLast('/')}"
    }
}
