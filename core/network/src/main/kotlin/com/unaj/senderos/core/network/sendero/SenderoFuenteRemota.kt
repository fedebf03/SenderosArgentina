package com.unaj.senderos.data.remote

// Fuente de datos remota de senderos; sus funciones lanzan excepción ante un fallo
interface SenderoFuenteRemota {
    suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed>

    suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed)
}
