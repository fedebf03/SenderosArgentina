package com.unaj.senderos.core.network.fotos

interface SubidorFotos {
    suspend fun subir(rutaLocal: String): String
}
