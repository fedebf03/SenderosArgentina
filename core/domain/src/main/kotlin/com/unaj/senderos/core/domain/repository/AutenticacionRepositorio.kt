package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.Usuario

interface AutenticacionRepositorio {
    suspend fun iniciarSesion(email: String, clave: String): Result<Usuario>

    suspend fun registrar(email: String, clave: String): Result<Usuario>

    suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario>

    // No revela si la cuenta existe
    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit>

    // Se pide la clave actual para confirmar que es el dueño de la cuenta
    suspend fun cambiarContrasena(claveActual: String, claveNueva: String): Result<Unit>

    // Las cuentas de Google no tienen una contraseña que cambiar
    fun tieneContrasena(): Boolean

    fun obtenerUsuarioActual(): Usuario?

    fun cerrarSesion()
}
