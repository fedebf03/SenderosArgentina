package com.unaj.senderos.falsos

import com.unaj.senderos.data.local.EjecutorTransacciones

class FalsoEjecutorTransacciones : EjecutorTransacciones {

    var cantidadLlamadas: Int = 0
        private set

    // Contar la llamada y ejecutar el bloque directamente
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
        cantidadLlamadas++
        return bloque()
    }
}
