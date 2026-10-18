package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.Usuario

interface AutenticacionRepositorio {
    suspend fun iniciarSesion(email: String, clave: String): Result<Usuario>

    suspend fun registrar(email: String, clave: String): Result<Usuario>

    // No revela si la cuenta existe
    suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario>

    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit>

    // Cambiar la clave de una cuenta de correo y clave: se pide la actual para confirmar que es el dueño
    suspend fun cambiarContrasena(claveActual: String, claveNueva: String): Result<Unit>

    // Indicar si la cuenta entra con correo y clave, y por eso tiene una contraseña que cambiar (las cuentas de Google no)
    fun tieneContrasena(): Boolean

    fun obtenerUsuarioActual(): Usuario?

    fun cerrarSesion()
}
