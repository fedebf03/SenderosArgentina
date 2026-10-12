package com.unaj.senderos.data.local

interface EjecutorTransacciones {
    // Ejecutar el bloque dentro de una transacción
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}
