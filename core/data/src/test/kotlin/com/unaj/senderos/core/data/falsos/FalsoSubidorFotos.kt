package com.unaj.senderos.falsos

import com.unaj.senderos.data.remote.SubidorFotos

class FalsoSubidorFotos : SubidorFotos {

    val rutasSubidas: MutableList<String> = mutableListOf()

    override suspend fun subir(rutaLocal: String): String {
        rutasSubidas.add(rutaLocal)
        return "https://imagenes.ejemplo/$rutaLocal"
    }
}
