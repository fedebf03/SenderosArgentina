package com.unaj.senderos.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderoDao {

    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estaEliminado = 0 ORDER BY nombre")
    fun observarPorUsuario(idUsuario: String): Flow<List<SenderoEntidad>>

    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero AND estaEliminado = 0")
    fun observarPorId(idUsuario: String, idSendero: String): Flow<SenderoEntidad?>

    // Obtener una vez un sendero del usuario, incluidos los eliminados, o null si no existe
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun obtenerPorId(idUsuario: String, idSendero: String): SenderoEntidad?

    // Obtener una vez los senderos del usuario que no están marcados como eliminados
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estaEliminado = 0")
    suspend fun obtenerActivos(idUsuario: String): List<SenderoEntidad>

    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(idUsuario: String): List<SenderoEntidad>

    @Upsert
    suspend fun guardar(entidad: SenderoEntidad)

    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun borrarDefinitivamente(idUsuario: String, idSendero: String)

    // Actualizar el estado solo si la fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("UPDATE senderos SET estadoSincronizacion = :estado WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarEstadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, estado: String): Int

    @Query("UPDATE senderos SET foto = :foto WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarFotoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, foto: String): Int

    @Query("UPDATE senderos SET recorrido = :recorrido WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarRecorridoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, recorrido: List<PuntoRecorridoEntidad>): Int

    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero AND estaEliminado = 1 AND fechaModificacion = :fechaLeida")
    suspend fun borrarEliminadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long): Int
}
