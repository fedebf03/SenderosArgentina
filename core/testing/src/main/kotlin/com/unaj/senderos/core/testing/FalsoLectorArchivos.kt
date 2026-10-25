package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.ArchivoLeido
import com.unaj.senderos.core.domain.port.LectorArchivos

class FalsoLectorArchivos : LectorArchivos {

    val archivos: MutableMap<String, ArchivoLeido> = mutableMapOf()

    override suspend fun leerTexto(uri: String): Result<ArchivoLeido> {
        return archivos[uri]?.let { archivo -> Result.success(archivo) }
            ?: Result.failure(IllegalStateException("No se pudo leer el archivo"))
    }
}
