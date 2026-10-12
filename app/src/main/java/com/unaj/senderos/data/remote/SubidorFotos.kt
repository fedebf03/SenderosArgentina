package com.unaj.senderos.data.remote

interface SubidorFotos {
    // Subir la foto y devolver su URL remota
    suspend fun subir(rutaLocal: String): String
}
