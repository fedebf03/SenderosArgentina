package com.unaj.senderos.core.data.repository

import com.unaj.senderos.core.data.mapeo.aDominio
import com.unaj.senderos.core.data.perfil.AlmacenPerfilLocal
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.network.auth.FuenteAutenticacion
import javax.inject.Inject

class AutenticacionRepositorioImpl @Inject constructor(
    private val fuenteAutenticacion: FuenteAutenticacion,
    private val almacenPerfil: AlmacenPerfilLocal
) : AutenticacionRepositorio {

    override suspend fun iniciarSesion(email: String, clave: String): Result<Usuario> =
        fuenteAutenticacion.iniciarSesion(email, clave).map { usuario -> usuario.aDominio().conPerfilLocal() }

    override suspend fun registrar(email: String, clave: String): Result<Usuario> =
        fuenteAutenticacion.registrar(email, clave).map { usuario -> usuario.aDominio().conPerfilLocal() }

    override suspend fun iniciarSesionConGoogle(idToken: String): Result<Usuario> =
        fuenteAutenticacion.iniciarSesionConGoogle(idToken).map { usuario -> usuario.aDominio().conPerfilLocal() }

    override suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> =
        fuenteAutenticacion.enviarCorreoRecuperacion(email)

    override suspend fun cambiarContrasena(claveActual: String, claveNueva: String): Result<Unit> =
        fuenteAutenticacion.cambiarContrasena(claveActual, claveNueva)

    override fun tieneContrasena(): Boolean = fuenteAutenticacion.tieneContrasena()

    override fun obtenerUsuarioActual(): Usuario? = fuenteAutenticacion.obtenerUsuarioActual()?.aDominio()?.conPerfilLocal()

    override fun cerrarSesion() = fuenteAutenticacion.cerrarSesion()

    // El nombre, la foto y el lugar que el usuario editó en la app mandan sobre los de su cuenta
    private fun Usuario.conPerfilLocal(): Usuario {
        val guardado = almacenPerfil.leer(id) ?: return this
        return copy(
            nombre = guardado.nombre.ifBlank { nombre },
            fotoUrl = guardado.fotoEfectiva.ifBlank { fotoUrl },
            pais = guardado.pais,
            provincia = guardado.provincia,
            localidad = guardado.localidad
        )
    }
}
