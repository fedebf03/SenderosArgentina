package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class FalsoAutenticacionRepositorio(
    var usuarioActual: Usuario? = null
) : AutenticacionRepositorio {

    val correosDeRecuperacion: MutableList<String> = mutableListOf()

    var resultadoRecuperacion: Result<Unit> = Result.success(Unit)

    var cantidadCierresSesion: Int = 0
        private set

    // Iniciar sesión con correo y contraseña
    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    // Registrar un usuario nuevo con correo y contraseña
    override suspend fun registrar(email: String, clave: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    // Iniciar sesión con el token de identidad de Google
    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    // Registrar el correo recibido y devolver el resultado configurado por la prueba
    override suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> {
        correosDeRecuperacion.add(email)
        return resultadoRecuperacion
    }

    // Obtener el usuario con sesión activa, si existe
    override fun obtenerUsuarioActual(): Usuario? {
        return usuarioActual
    }

    // Cerrar la sesión del usuario actual
    override fun cerrarSesion() {
        cantidadCierresSesion++
        usuarioActual = null
    }
}
