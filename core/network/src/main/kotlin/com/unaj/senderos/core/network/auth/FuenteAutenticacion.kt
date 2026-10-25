package com.unaj.senderos.core.network.auth

interface FuenteAutenticacion {
    suspend fun iniciarSesion(email: String, clave: String): Result<UsuarioRed>
    suspend fun registrar(email: String, clave: String): Result<UsuarioRed>
    suspend fun iniciarSesionConGoogle(idToken: String): Result<UsuarioRed>
    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit>
    suspend fun actualizarNombre(nombre: String): Result<Unit>
    suspend fun cambiarContrasena(claveActual: String, claveNueva: String): Result<Unit>
    fun tieneContrasena(): Boolean
    fun obtenerUsuarioActual(): UsuarioRed?
    fun cerrarSesion()
}
