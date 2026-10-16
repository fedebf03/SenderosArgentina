package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.Usuario

interface AutenticacionRepositorio {
    suspend fun iniciarSesion(email: String, clave: String): Result<Usuario>

    suspend fun registrar(email: String, clave: String): Result<Usuario>

    // No revela si la cuenta existe
    suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario>

    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit>

    fun obtenerUsuarioActual(): Usuario?

    fun cerrarSesion()
}
