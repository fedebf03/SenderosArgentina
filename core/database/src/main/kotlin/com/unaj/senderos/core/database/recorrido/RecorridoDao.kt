package com.unaj.senderos.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecorridoDao {

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario AND estaEliminado = 0 ORDER BY inicioMilisegundos DESC")
    fun observarPorUsuario(idUsuario: String): Flow<List<RecorridoEntidad>>

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario AND id = :idRecorrido AND estaEliminado = 0")
    fun observarPorId(idUsuario: String, idRecorrido: String): Flow<RecorridoEntidad?>

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario AND id = :idRecorrido")
    suspend fun obtenerPorId(idUsuario: String, idRecorrido: String): RecorridoEntidad?

    @Query("SELECT * FROM recorridos WHERE idUsuario = :idUsuario AND estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(idUsuario: String): List<RecorridoEntidad>

    @Upsert
    suspend fun guardar(entidad: RecorridoEntidad)

    // Quitar la fila de un recorrido eliminado solo si su fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("DELETE FROM recorridos WHERE idUsuario = :idUsuario AND id = :idRecorrido AND estaEliminado = 1 AND fechaModificacion = :fechaLeida")
    suspend fun borrarEliminadoSiNoCambio(idUsuario: String, idRecorrido: String, fechaLeida: Long): Int

    // Actualizar el estado solo si la fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("UPDATE recorridos SET estadoSincronizacion = :estado WHERE idUsuario = :idUsuario AND id = :idRecorrido AND fechaModificacion = :fechaLeida")
    suspend fun actualizarEstadoSiNoCambio(idUsuario: String, idRecorrido: String, fechaLeida: Long, estado: String): Int
}
