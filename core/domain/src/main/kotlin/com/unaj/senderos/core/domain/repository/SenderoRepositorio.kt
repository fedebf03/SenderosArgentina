package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Sendero

interface SenderoRepositorio {
    // Obtener los senderos que pertenecen al usuario indicado
    suspend fun obtenerSenderosDelUsuario(idUsuario: String): Result<List<Sendero>>
}
