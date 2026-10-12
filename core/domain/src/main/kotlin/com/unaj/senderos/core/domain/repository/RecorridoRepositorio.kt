package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.RecorridoRealizado
import kotlinx.coroutines.flow.Flow

interface RecorridoRepositorio {

    // Observar los recorridos del usuario, del más reciente al más viejo
    fun observarRecorridos(idUsuario: String): Flow<List<RecorridoRealizado>>

    // Observar un recorrido del usuario; emite null si no existe
    fun observarRecorrido(idUsuario: String, idRecorrido: String): Flow<RecorridoRealizado?>

    // Guardar el recorrido en la fuente local como pendiente de sincronizar
    suspend fun guardar(recorrido: RecorridoRealizado): Result<Unit>
}
