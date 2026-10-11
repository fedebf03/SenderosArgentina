package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Usuario

interface AutenticacionRepositorio {
    suspend fun iniciarSesion(email: String, clave: String): Result<Usuario>

    suspend fun registrar(email: String, clave: String): Result<Usuario>

    suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario>

    fun obtenerUsuarioActual(): Usuario?

    fun cerrarSesion()
}
