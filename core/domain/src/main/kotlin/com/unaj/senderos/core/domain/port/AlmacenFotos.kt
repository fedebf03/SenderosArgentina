package com.unaj.senderos.core.domain.port

interface AlmacenFotos {
    suspend fun guardarCopia(uriOrigen: String): Result<String>

    // Se puede llamar más de una vez y no falla si el archivo no existe o no se puede borrar
    suspend fun borrar(ruta: String)
}
