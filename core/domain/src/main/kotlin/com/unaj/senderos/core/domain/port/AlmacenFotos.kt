package com.unaj.senderos.domain.repository

interface AlmacenFotos {
    // Guardar una copia local de la imagen y devolver su ruta
    suspend fun guardarCopia(uriOrigen: String): Result<String>

    // Borrar el archivo de una copia guardada; es idempotente y no falla si el archivo no existe o no se puede borrar
    suspend fun borrar(ruta: String)
}
