package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Usuario

interface AutenticacionRepositorio {
    // Iniciar sesión con correo y contraseña
    suspend fun iniciarSesion(email: String, clave: String): Result<Usuario>

    // Registrar un usuario nuevo con correo y contraseña
    suspend fun registrar(email: String, clave: String): Result<Usuario>

    // Iniciar sesión con el token de identidad de Google
    suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario>

    // Enviar el correo de recuperación de contraseña sin revelar si la cuenta existe
    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit>

    // Obtener el usuario con sesión activa, si existe
    fun obtenerUsuarioActual(): Usuario?

    // Cerrar la sesión del usuario actual
    fun cerrarSesion()
}
