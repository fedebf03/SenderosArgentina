package com.unaj.senderos.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoCapturadoDao {

    // Observar los puntos capturados de un sendero del usuario en orden de captura
    @Query("SELECT * FROM puntos_capturados WHERE idUsuario = :idUsuario AND idSendero = :idSendero ORDER BY instanteMilisegundos")
    fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturadoEntidad>>

    // Obtener una vez los puntos capturados de un sendero del usuario en orden de captura
    @Query("SELECT * FROM puntos_capturados WHERE idUsuario = :idUsuario AND idSendero = :idSendero ORDER BY instanteMilisegundos")
    suspend fun obtenerPendientes(idUsuario: String, idSendero: String): List<PuntoCapturadoEntidad>

    // Obtener una vez los puntos del usuario con los identificadores indicados
    @Query("SELECT * FROM puntos_capturados WHERE idUsuario = :idUsuario AND id IN (:ids)")
    suspend fun obtenerPorIds(idUsuario: String, ids: List<String>): List<PuntoCapturadoEntidad>

    // Insertar o reemplazar el punto con la misma clave compuesta
    @Upsert
    suspend fun guardar(entidad: PuntoCapturadoEntidad)

    // Borrar los puntos del usuario con los identificadores indicados
    @Query("DELETE FROM puntos_capturados WHERE idUsuario = :idUsuario AND id IN (:ids)")
    suspend fun borrar(idUsuario: String, ids: List<String>)
}
