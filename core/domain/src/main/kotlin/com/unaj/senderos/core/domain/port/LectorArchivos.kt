package com.unaj.senderos.core.domain.port

data class ArchivoLeido(val nombre: String, val texto: String)

interface LectorArchivos {
    // Falla si no se puede leer o si el archivo es demasiado grande para un trazado
    suspend fun leerTexto(uri: String): Result<ArchivoLeido>
}
