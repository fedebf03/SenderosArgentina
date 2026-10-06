package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Usuario

interface AutenticacionRepositorio {
    // Autenticar al usuario con correo electrónico y contraseña en Firebase
    suspend fun iniciarSesion(email: String, clave: String): Result<Usuario>

    // Crear una cuenta nueva con correo electrónico y contraseña en Firebase
    suspend fun registrar(email: String, clave: String): Result<Usuario>

    // Autenticar al usuario con el token de ID de Google en Firebase
    suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario>

    // Obtener la información del usuario con sesión activa en Firebase
    fun obtenerUsuarioActual(): Usuario?

    // Finalizar la sesión actual del usuario autenticado
    fun cerrarSesion()
}
