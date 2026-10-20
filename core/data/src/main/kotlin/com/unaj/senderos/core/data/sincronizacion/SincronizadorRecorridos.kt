package com.unaj.senderos.core.data.sincronizacion

import android.util.Log
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.recorrido.RecorridoDao
import com.unaj.senderos.core.database.recorrido.RecorridoEntidad
import com.unaj.senderos.core.data.mapeo.aEntidad
import com.unaj.senderos.core.data.mapeo.aRed
import com.unaj.senderos.core.network.recorrido.RecorridoFuenteRemota
import com.unaj.senderos.core.network.recorrido.RecorridoRed
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

private const val TAG = "SINCRONIZACION_RECORRIDOS"

// Room es la fuente de verdad. De Firestore solo se toman los recorridos más nuevos que la copia local
class SincronizadorRecorridos @Inject constructor(
    private val recorridoDao: RecorridoDao,
    private val fuenteRemota: RecorridoFuenteRemota,
    private val ejecutorTransacciones: EjecutorTransacciones
) {
    suspend fun sincronizar(idUsuario: String): Result<Unit> {
        Log.d(TAG, "Comienza la sincronización de recorridos del usuario $idUsuario")
        return try {
            fusionarRemotos(idUsuario, fuenteRemota.obtenerRecorridos(idUsuario))
            val primerError = subirPendientes(idUsuario)
            if (primerError == null) Result.success(Unit) else Result.failure(primerError)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo la sincronización de recorridos del usuario $idUsuario", error)
            Result.failure(error)
        }
    }

    private suspend fun fusionarRemotos(idUsuario: String, remotos: List<RecorridoRed>) {
        remotos.forEach { remoto ->
            ejecutorTransacciones.ejecutar {
                val local = recorridoDao.obtenerPorId(idUsuario, remoto.id)
                if (local == null || remoto.fechaModificacion > local.fechaModificacion) {
                    recorridoDao.guardar(remoto.aEntidad(idUsuario))
                }
            }
        }
    }

    // Cada pendiente se sube una sola vez. Los que fallan los reintenta el worker en su próxima ejecución
    private suspend fun subirPendientes(idUsuario: String): Throwable? {
        var primerError: Throwable? = null
        recorridoDao.obtenerPendientes(idUsuario).forEach { entidad ->
            val error = subirEntidad(idUsuario, entidad)
            if (error != null && primerError == null) primerError = error
        }
        return primerError
    }

    // El estado cambia solo si la fila no se modificó mientras se subía. Si se editó en el medio, sigue pendiente
    private suspend fun subirEntidad(idUsuario: String, entidad: RecorridoEntidad): Throwable? {
        return try {
            if (entidad.estaEliminado) {
                // La fila local se borra recién con el documento remoto borrado, y solo si nadie la tocó mientras tanto
                fuenteRemota.eliminarRecorrido(idUsuario, entidad.id)
                recorridoDao.borrarEliminadoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion)
                return null
            }
            fuenteRemota.guardarRecorrido(idUsuario, entidad.aRed())
            recorridoDao.actualizarEstadoSiNoCambio(
                idUsuario,
                entidad.id,
                entidad.fechaModificacion,
                EstadoSincronizacion.SINCRONIZADO.name
            )
            null
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo al subir el recorrido ${entidad.id}", error)
            recorridoDao.actualizarEstadoSiNoCambio(
                idUsuario,
                entidad.id,
                entidad.fechaModificacion,
                EstadoSincronizacion.CON_ERROR.name
            )
            error
        }
    }
}
