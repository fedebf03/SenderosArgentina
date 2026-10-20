package com.unaj.senderos.core.network.recorrido

// Las funciones lanzan una excepción ante un fallo
interface RecorridoFuenteRemota {
    suspend fun obtenerRecorridos(idUsuario: String): List<RecorridoRed>

    suspend fun guardarRecorrido(idUsuario: String, recorrido: RecorridoRed)

    // Si el documento no existe no es un error
    suspend fun eliminarRecorrido(idUsuario: String, idRecorrido: String)
}
