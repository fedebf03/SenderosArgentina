package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.network.fotos.SubidorFotos
class FalsoSubidorFotos : SubidorFotos {

    val rutasSubidas: MutableList<String> = mutableListOf()

    override suspend fun subir(rutaLocal: String): String {
        rutasSubidas.add(rutaLocal)
        return "https://imagenes.ejemplo/$rutaLocal"
    }
}
