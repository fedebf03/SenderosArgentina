package com.unaj.senderos.core.network.sendero

// Las funciones lanzan una excepción ante un fallo
interface SenderoFuenteRemota {
    suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed>

    suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed)
}
