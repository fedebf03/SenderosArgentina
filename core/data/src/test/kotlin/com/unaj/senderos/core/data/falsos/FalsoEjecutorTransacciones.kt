package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.database.EjecutorTransacciones

class FalsoEjecutorTransacciones : EjecutorTransacciones {

    var cantidadLlamadas: Int = 0
        private set

    override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
        cantidadLlamadas++
        return bloque()
    }
}
