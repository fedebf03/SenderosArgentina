package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.RecorridoRealizado

// Guarda cada tanto el recorrido en curso para no perderlo si Android cierra la app; es una copia local, no se sincroniza
interface AlmacenBorradorRecorrido {
    // Reemplazar el borrador anterior por el estado más reciente del recorrido en curso
    suspend fun guardar(borrador: RecorridoRealizado): Result<Unit>

    // Leer el borrador que quedó guardado; nulo si no hay o si el archivo está dañado
    suspend fun leer(): RecorridoRealizado?

    // Borrar el borrador cuando el recorrido terminó o se canceló
    suspend fun borrar()
}
