package com.unaj.senderos.data.local

import androidx.room.withTransaction

// Implementación sobre Room que agrupa el bloque en una transacción de la base de datos
class EjecutorTransaccionesRoom(
    private val baseDatos: BaseDatosSenderos
) : EjecutorTransacciones {

    // Ejecutar el bloque dentro de una transacción de Room
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
        return baseDatos.withTransaction(bloque)
    }
}
