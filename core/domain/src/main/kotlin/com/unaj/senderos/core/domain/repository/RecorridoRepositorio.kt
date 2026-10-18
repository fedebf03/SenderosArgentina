package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import kotlinx.coroutines.flow.Flow

interface RecorridoRepositorio {

    // Del más reciente al más viejo
    fun observarRecorridos(idUsuario: String): Flow<List<RecorridoRealizado>>

    // Observar un recorrido del usuario; emite null si no existe
    fun observarRecorrido(idUsuario: String, idRecorrido: String): Flow<RecorridoRealizado?>

    // Queda pendiente de sincronizar
    suspend fun guardar(recorrido: RecorridoRealizado): Result<Unit>
    // Borrar un recorrido del historial: se oculta al instante y se borra de la nube en cuanto hay conexión; no falla si no existe
    suspend fun eliminar(idUsuario: String, idRecorrido: String): Result<Unit>
}
