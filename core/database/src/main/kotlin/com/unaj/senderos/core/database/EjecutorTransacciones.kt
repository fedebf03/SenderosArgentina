package com.unaj.senderos.data.local

// Abstracción para ejecutar varias operaciones locales de forma atómica
interface EjecutorTransacciones {
    // Ejecutar el bloque dentro de una única transacción y devolver su resultado
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}
