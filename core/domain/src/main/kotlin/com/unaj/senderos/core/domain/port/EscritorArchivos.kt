package com.unaj.senderos.core.domain.port

interface EscritorArchivos {
    // Escribir el texto en el archivo que el usuario eligió para guardar (una Uri del selector del sistema);
    // falla con un mensaje en español si no se puede
    suspend fun escribirTexto(uri: String, texto: String): Result<Unit>
}
