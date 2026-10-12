package com.unaj.senderos.falsos

import com.unaj.senderos.data.remote.SubidorFotos

class FalsoSubidorFotos : SubidorFotos {

    val rutasSubidas: MutableList<String> = mutableListOf()

    // Registrar la ruta recibida y devolver una URL remota determinística
    override suspend fun subir(rutaLocal: String): String {
        rutasSubidas.add(rutaLocal)
        return "https://imagenes.ejemplo/$rutaLocal"
    }
}
