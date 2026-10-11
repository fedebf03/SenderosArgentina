package com.unaj.senderos.data.remote

interface SubidorFotos {
    suspend fun subir(rutaLocal: String): String
}
