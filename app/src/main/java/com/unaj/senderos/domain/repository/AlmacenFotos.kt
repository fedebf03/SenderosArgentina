package com.unaj.senderos.domain.repository

interface AlmacenFotos {
    suspend fun guardarCopia(uriOrigen: String): Result<String>
}
