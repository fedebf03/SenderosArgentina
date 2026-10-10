package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class FalsoAutenticacionRepositorio(
    var usuarioActual: Usuario? = null
) : AutenticacionRepositorio {

    var cantidadCierresSesion: Int = 0
        private set

    // Simular el inicio de sesión con correo devolviendo el usuario configurado
    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    // Simular el registro de una cuenta devolviendo el usuario configurado
    override suspend fun registrar(email: String, clave: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    // Simular el inicio de sesión con Google devolviendo el usuario configurado
    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    // Obtener el usuario configurado como sesión activa
    override fun obtenerUsuarioActual(): Usuario? {
        return usuarioActual
    }

    // Registrar la invocación del cierre de sesión y limpiar el usuario activo
    override fun cerrarSesion() {
        cantidadCierresSesion++
        usuarioActual = null
    }
}
