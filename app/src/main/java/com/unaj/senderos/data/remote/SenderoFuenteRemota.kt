package com.unaj.senderos.data.remote

// Fuente de datos remota de senderos; sus funciones lanzan excepción ante un fallo
interface SenderoFuenteRemota {
    // Obtener todos los senderos remotos del usuario
    suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed>

    // Crear o reemplazar el documento del sendero del usuario
    suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed)

    // Consultar si ya se cargaron los senderos de ejemplo del usuario
    suspend fun obtenerMarcaEjemplosCargados(idUsuario: String): Boolean

    // Registrar que ya se cargaron los senderos de ejemplo del usuario
    suspend fun registrarMarcaEjemplosCargados(idUsuario: String)
}
