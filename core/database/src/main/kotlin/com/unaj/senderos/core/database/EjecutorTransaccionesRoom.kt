package com.unaj.senderos.core.database

import androidx.room.withTransaction
import javax.inject.Inject

class EjecutorTransaccionesRoom @Inject constructor(
    private val baseDatos: BaseDatosSenderos
) : EjecutorTransacciones {

    override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
        return baseDatos.withTransaction(bloque)
    }
}
