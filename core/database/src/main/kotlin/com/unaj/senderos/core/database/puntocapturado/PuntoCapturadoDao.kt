package com.unaj.senderos.core.database.puntocapturado

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoCapturadoDao {

    @Query("SELECT * FROM puntos_capturados WHERE idUsuario = :idUsuario AND idSendero = :idSendero ORDER BY instanteMilisegundos")
    fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturadoEntidad>>

    @Query("SELECT * FROM puntos_capturados WHERE idUsuario = :idUsuario AND idSendero = :idSendero ORDER BY instanteMilisegundos")
    suspend fun obtenerPendientes(idUsuario: String, idSendero: String): List<PuntoCapturadoEntidad>

    @Query("SELECT * FROM puntos_capturados WHERE idUsuario = :idUsuario AND id IN (:ids)")
    suspend fun obtenerPorIds(idUsuario: String, ids: List<String>): List<PuntoCapturadoEntidad>

    @Upsert
    suspend fun guardar(entidad: PuntoCapturadoEntidad)

    @Query("DELETE FROM puntos_capturados WHERE idUsuario = :idUsuario AND id IN (:ids)")
    suspend fun borrar(idUsuario: String, ids: List<String>)
}
