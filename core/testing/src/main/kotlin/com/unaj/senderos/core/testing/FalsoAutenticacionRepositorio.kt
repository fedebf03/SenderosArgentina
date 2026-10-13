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

    override suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> {
        correosDeRecuperacion.add(email)
        return resultadoRecuperacion
    }

    override fun obtenerUsuarioActual(): Usuario? {
        return usuarioActual
    }

    override fun cerrarSesion() {
        cantidadCierresSesion++
        usuarioActual = null
    }
}
