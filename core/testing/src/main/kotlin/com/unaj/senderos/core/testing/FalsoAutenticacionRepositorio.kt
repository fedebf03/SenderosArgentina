package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class FalsoAutenticacionRepositorio(
    var usuarioActual: Usuario? = null
) : AutenticacionRepositorio {

    var cantidadCierresSesion: Int = 0
        private set

    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    override suspend fun registrar(email: String, clave: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> {
        return usuarioActual?.let { usuario -> Result.success(usuario) }
            ?: Result.failure(IllegalStateException("Sin usuario configurado"))
    }

    override fun obtenerUsuarioActual(): Usuario? {
        return usuarioActual
    }

    override fun cerrarSesion() {
        cantidadCierresSesion++
        usuarioActual = null
    }
}
