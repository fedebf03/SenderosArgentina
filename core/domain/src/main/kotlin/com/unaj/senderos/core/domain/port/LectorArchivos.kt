package com.unaj.senderos.core.domain.port

// Contenido de texto de un archivo elegido por el usuario, con su nombre visible
data class ArchivoLeido(val nombre: String, val texto: String)

interface LectorArchivos {
    // Leer como texto el archivo elegido (una Uri del selector de archivos); falla con un mensaje en español si no se puede
    // leer o si es demasiado grande para un trazado
    suspend fun leerTexto(uri: String): Result<ArchivoLeido>
}
