package com.unaj.senderos.core.data.sincronizacion

import android.util.Log
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.RecorridoDao
import com.unaj.senderos.core.database.RecorridoEntidad
import com.unaj.senderos.core.data.mapeo.aEntidad
import com.unaj.senderos.core.data.mapeo.aRed
import com.unaj.senderos.core.network.RecorridoFuenteRemota
import com.unaj.senderos.core.network.RecorridoRed
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

private const val TAG = "SINCRONIZACION_RECORRIDOS"

// La base local es la fuente de verdad frente a Firestore
class SincronizadorRecorridos @Inject constructor(
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
            if (entidad.estaEliminado) {
                // El borrado se completa con el documento remoto ya borrado; la fila local se quita solo si nadie la tocó durante tanto tiempo
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
