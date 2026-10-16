package com.unaj.senderos.core.domain.port

interface AlmacenFotos {
    suspend fun guardarCopia(uriOrigen: String): Result<String>

    // Borrar el archivo de una copia guardada; es idempotente y no falla si el archivo no existe o no se puede borrar
    suspend fun borrar(ruta: String)
}
