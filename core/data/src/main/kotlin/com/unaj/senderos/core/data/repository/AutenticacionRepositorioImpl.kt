package com.unaj.senderos.core.data.repository

import com.unaj.senderos.core.data.mapeo.aDominio
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.network.FuenteAutenticacion
import javax.inject.Inject

class AutenticacionRepositorioImpl @Inject constructor(
    private val fuenteAutenticacion: FuenteAutenticacion
) : AutenticacionRepositorio {

    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> =
        fuenteAutenticacion.iniciarSesion(email, clave).map { usuario -> usuario.aDominio() }

    override suspend fun registrar(email: String, clave: String): Result<Usuario> =
        fuenteAutenticacion.registrar(email, clave).map { usuario -> usuario.aDominio() }

    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> =
        fuenteAutenticacion.iniciarSesionConGoogle(idToken).map { usuario -> usuario.aDominio() }

    override suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> =
        fuenteAutenticacion.enviarCorreoRecuperacion(email)

    override fun obtenerUsuarioActual(): Usuario? = fuenteAutenticacion.obtenerUsuarioActual()?.aDominio()

    override fun cerrarSesion() = fuenteAutenticacion.cerrarSesion()
}
