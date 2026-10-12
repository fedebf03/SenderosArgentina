package com.unaj.senderos.data.sincronizacion

import android.util.Log
import com.unaj.senderos.data.local.EjecutorTransacciones
import com.unaj.senderos.data.local.RecorridoDao
import com.unaj.senderos.data.local.RecorridoEntidad
import com.unaj.senderos.data.mapeo.aEntidad
import com.unaj.senderos.data.mapeo.aRed
import com.unaj.senderos.data.remote.RecorridoFuenteRemota
import com.unaj.senderos.data.remote.RecorridoRed
import com.unaj.senderos.domain.model.EstadoSincronizacion
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SINCRONIZACION_RECORRIDOS"

// Sincronizar los recorridos locales (fuente de verdad) con Firestore: pull, fusión por fecha y subida de pendientes
class SincronizadorRecorridos(
    private val recorridoDao: RecorridoDao,
    private val fuenteRemota: RecorridoFuenteRemota,
    private val ejecutorTransacciones: EjecutorTransacciones
) {
    // Sincronizar al usuario; devolver failure con el primer error si falla la lectura remota o alguna subida
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

    // Fusionar los documentos remotos con los locales aplicando que la última escritura prevalece
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

    // Subir una vez cada pendiente sin reintentar los fallidos; devolver el primer error o null
    private suspend fun subirPendientes(idUsuario: String): Throwable? {
        var primerError: Throwable? = null
        recorridoDao.obtenerPendientes(idUsuario).forEach { entidad ->
            val error = subirEntidad(idUsuario, entidad)
            if (error != null && primerError == null) primerError = error
        }
        return primerError
    }

    // Subir una entidad y transicionar su estado solo si no cambió durante la subida; devolver el error o null
    private suspend fun subirEntidad(idUsuario: String, entidad: RecorridoEntidad): Throwable? {
        return try {
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
