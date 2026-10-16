package com.unaj.senderos.core.database

interface EjecutorTransacciones {
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}
