package com.unaj.senderos.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecorridoDao {

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario ORDER BY inicioMilisegundos DESC")
    fun observarPorUsuario(idUsuario: String): Flow<List<RecorridoEntidad>>

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario AND id = :idRecorrido")
    fun observarPorId(idUsuario: String, idRecorrido: String): Flow<RecorridoEntidad?>

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario AND id = :idRecorrido")
    suspend fun obtenerPorId(idUsuario: String, idRecorrido: String): RecorridoEntidad?

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario AND estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(idUsuario: String): List<RecorridoEntidad>

    @Upsert
    suspend fun guardar(entidad: RecorridoEntidad)

    // Actualizar el estado solo si la fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("UPDATE recorridos SET estadoSincronizacion = :estado WHERE idUsuario = :idUsuario AND id = :idRecorrido AND fechaModificacion = :fechaLeida")
    suspend fun actualizarEstadoSiNoCambio(idUsuario: String, idRecorrido: String, fechaLeida: Long, estado: String): Int
}
