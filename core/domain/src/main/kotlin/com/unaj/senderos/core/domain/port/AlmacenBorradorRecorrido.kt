package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.RecorridoRealizado

// Guardamos cada tanto el recorrido en curso para no perderlo si Android cierra la app. Es una copia local
// que no se sincroniza
interface AlmacenBorradorRecorrido {
    suspend fun guardar(borrador: RecorridoRealizado): Result<Unit>

    // También devuelve nulo si el archivo está dañado
    suspend fun leer(): RecorridoRealizado?

    suspend fun borrar()
}
