package com.unaj.senderos.core.database.sendero

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

    // A diferencia de observarPorId, incluye los senderos eliminados
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun obtenerPorId(idUsuario: String, idSendero: String): SenderoEntidad?

    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estaEliminado = 0")
    suspend fun obtenerActivos(idUsuario: String): List<SenderoEntidad>

    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(idUsuario: String): List<SenderoEntidad>

    @Upsert
    suspend fun guardar(entidad: SenderoEntidad)

    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun borrarDefinitivamente(idUsuario: String, idSendero: String)

    // Si el sendero cambió durante la subida, la fecha ya no coincide y el estado no se toca
    @Query("UPDATE senderos SET estadoSincronizacion = :estado WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarEstadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, estado: String): Int

    @Query("UPDATE senderos SET foto = :foto WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarFotoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, foto: String): Int

    @Query("UPDATE senderos SET recorrido = :recorrido WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarRecorridoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, recorrido: List<PuntoRecorridoEntidad>): Int

    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero AND estaEliminado = 1 AND fechaModificacion = :fechaLeida")
    suspend fun borrarEliminadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long): Int
}
