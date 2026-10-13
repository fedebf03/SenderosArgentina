package com.unaj.senderos.data.local

import androidx.room.withTransaction

class EjecutorTransaccionesRoom(
    private val baseDatos: BaseDatosSenderos
) : EjecutorTransacciones {

    override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
        return baseDatos.withTransaction(bloque)
    }
}
