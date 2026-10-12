package com.unaj.senderos.data.remote

// Fuente de datos remota de recorridos; sus funciones lanzan excepción ante un fallo
interface RecorridoFuenteRemota {
    // Obtener del servidor todos los recorridos del usuario; lanzar excepción si falla
    suspend fun obtenerRecorridos(idUsuario: String): List<RecorridoRed>

    // Guardar el recorrido completo del usuario en el servidor; lanzar excepción si falla
    suspend fun guardarRecorrido(idUsuario: String, recorrido: RecorridoRed)
}
