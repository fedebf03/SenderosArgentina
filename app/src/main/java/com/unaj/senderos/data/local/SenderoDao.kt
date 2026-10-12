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

    // Observar un sendero no eliminado del usuario; emite null si no existe
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero AND estaEliminado = 0")
    fun observarPorId(idUsuario: String, idSendero: String): Flow<SenderoEntidad?>

    // Obtener una vez un sendero del usuario, incluidos los eliminados, o null si no existe
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun obtenerPorId(idUsuario: String, idSendero: String): SenderoEntidad?

    // Obtener los senderos del usuario cuyo estado no es SINCRONIZADO
    @Query("SELECT * FROM senderos WHERE idUsuario = :idUsuario AND estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(idUsuario: String): List<SenderoEntidad>

    // Insertar o reemplazar el sendero con la misma clave compuesta
    @Upsert
    suspend fun guardar(entidad: SenderoEntidad)

    // Borrar la fila del sendero sin importar su estado
    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero")
    suspend fun borrarDefinitivamente(idUsuario: String, idSendero: String)

    // Actualizar el estado solo si la fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("UPDATE senderos SET estadoSincronizacion = :estado WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarEstadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, estado: String): Int

    // Actualizar la foto solo si la fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("UPDATE senderos SET foto = :foto WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarFotoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, foto: String): Int

    // Actualizar el recorrido solo si la fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("UPDATE senderos SET recorrido = :recorrido WHERE idUsuario = :idUsuario AND id = :idSendero AND fechaModificacion = :fechaLeida")
    suspend fun actualizarRecorridoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long, recorrido: List<PuntoRecorridoEntidad>): Int

    // Borrar el sendero eliminado lógico solo si la fecha de modificación sigue igual a la leída; devolver filas afectadas
    @Query("DELETE FROM senderos WHERE idUsuario = :idUsuario AND id = :idSendero AND estaEliminado = 1 AND fechaModificacion = :fechaLeida")
    suspend fun borrarEliminadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long): Int
}
