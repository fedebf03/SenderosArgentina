package com.unaj.senderos.core.network

interface SubidorFotos {
    // Subir la foto y devolver su URL remota
    suspend fun subir(rutaLocal: String): String
}
