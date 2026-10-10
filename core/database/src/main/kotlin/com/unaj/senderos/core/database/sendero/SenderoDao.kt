package com.unaj.senderos.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderoDao {

    // Observar los senderos no eliminados del usuario ordenados por nombre
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estaEliminado = 0 ORDER BY nombre")
    fun observarPorUsuario(idUsuario: String): Flow<List<SenderoEntidad>>

    // Observar un sendero no eliminado del usuario o null si no existe
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero AND estaEliminado = 0")
    fun observarPorId(idUsuario: String, idSendero: String): Flow<SenderoEntidad?>

    // Obtener un sendero del usuario incluyendo los eliminados
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun obtenerPorId(idUsuario: String, idSendero: String): SenderoEntidad?

    // Obtener todos los senderos del usuario incluyendo los eliminados
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario")
    suspend fun obtenerTodosDelUsuario(idUsuario: String): List<SenderoEntidad>

    // Obtener los senderos del usuario cuyo estado de sincronización no es SINCRONIZADO
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(idUsuario: String): List<SenderoEntidad>

    // Insertar o reemplazar la entidad con la misma clave compuesta
    @Upsert
    suspend fun guardar(entidad: SenderoEntidad)

    // Borrar definitivamente la fila del usuario con el identificador indicado
    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun borrarDefinitivamente(idUsuario: String, idSendero: String)

    // Actualizar el estado de sincronización solo si la fecha de modificación sigue siendo la leída; devolver filas afectadas
    @Query("UPDATE senderos SET estadoSincronizacion = :estado WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarEstadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, estado: String): Int

    // Borrar el sendero eliminado lógicamente solo si la fecha de modificación sigue siendo la leída; devolver filas afectadas
    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero AND estaEliminado = 1 AND fechaModificacion = :fechaLeida")
    suspend fun borrarEliminadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long): Int
}
