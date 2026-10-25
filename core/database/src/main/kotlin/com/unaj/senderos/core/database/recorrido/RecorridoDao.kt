package com.unaj.senderos.core.database.recorrido

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

    // Solo se quita la fila si la fecha de modificación sigue igual a la leída, por si hubo un cambio durante la subida
    @Query("DELETE FROM recorridos WHERE idUsuario = :idUsuario AND id = :idRecorrido AND estaEliminado = 1 AND fechaModificacion = :fechaLeida")
    suspend fun borrarEliminadoSiNoCambio(idUsuario: String, idRecorrido: String, fechaLeida: Long): Int

    // Si el recorrido cambió durante la subida, la fecha ya no coincide y el estado no se toca
    @Query("UPDATE recorridos SET estadoSincronizacion = :estado WHERE idUsuario = :idUsuario AND id = :idRecorrido AND fechaModificacion = :fechaLeida")
    suspend fun actualizarEstadoSiNoCambio(idUsuario: String, idRecorrido: String, fechaLeida: Long, estado: String): Int
}
