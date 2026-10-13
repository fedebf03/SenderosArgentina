package com.unaj.senderos.data.local

interface EjecutorTransacciones {
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}
