package com.unaj.senderos.core.network

interface FuenteAutenticacion {
    suspend fun iniciarSesion(email: String, clave: String): Result<UsuarioRed>
    suspend fun registrar(email: String, clave: String): Result<UsuarioRed>
    suspend fun iniciarSesionConGoogle(idToken: String): Result<UsuarioRed>
    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit>
    fun obtenerUsuarioActual(): UsuarioRed?
    fun cerrarSesion()
}
