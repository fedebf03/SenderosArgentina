package com.unaj.senderos.data.remote

// Fuente de datos remota de recorridos; sus funciones lanzan excepción ante un fallo
interface RecorridoFuenteRemota {
    suspend fun obtenerRecorridos(idUsuario: String): List<RecorridoRed>

    suspend fun guardarRecorrido(idUsuario: String, recorrido: RecorridoRed)
}
