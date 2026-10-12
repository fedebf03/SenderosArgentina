package com.unaj.senderos.data.remote

// Fuente de datos remota de senderos; sus funciones lanzan excepción ante un fallo
interface SenderoFuenteRemota {
    // Obtener los senderos remotos del usuario
    suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed>

    // Guardar un sendero del usuario
    suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed)
}
