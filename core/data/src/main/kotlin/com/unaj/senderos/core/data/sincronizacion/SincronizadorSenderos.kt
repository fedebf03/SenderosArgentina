package com.unaj.senderos.data.sincronizacion

import android.util.Log
import com.unaj.senderos.data.local.EjecutorTransacciones
import com.unaj.senderos.data.local.SenderoDao
import com.unaj.senderos.data.local.SenderoEntidad
import com.unaj.senderos.data.mapeo.aEntidad
import com.unaj.senderos.data.mapeo.aRed
import com.unaj.senderos.data.remote.SenderoFuenteRemota
import com.unaj.senderos.data.remote.SenderoRed
import com.unaj.senderos.data.remote.SubidorFotos
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.esFotoLocal
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SINCRONIZACION"
private const val MAXIMO_PASADAS_SUBIDA = 3

// Sincronizar la base local (fuente de verdad) con Firestore: pull, fusión por fecha y subida diferida
class SincronizadorSenderos(
    private val senderoDao: SenderoDao,
    private val fuenteRemota: SenderoFuenteRemota,
    private val ejecutorTransacciones: EjecutorTransacciones,
    private val subidorFotos: SubidorFotos
) {
    suspend fun sincronizar(idUsuario: String): Result<Unit> {
        Log.d(TAG, "Comienza la sincronización del usuario $idUsuario")
        return try {
            val resultado = sincronizarSinCapturar(idUsuario)
            if (resultado.isSuccess) {
                Log.d(TAG, "Termina la sincronización del usuario $idUsuario")
            } else {
                Log.e(TAG, "La sincronización del usuario $idUsuario terminó con fallos", resultado.exceptionOrNull())
            }
            resultado
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo la sincronización del usuario $idUsuario", error)
            Result.failure(error)
        }
    }

    private suspend fun sincronizarSinCapturar(idUsuario: String): Result<Unit> {
        fusionarRemotos(idUsuario, fuenteRemota.obtenerSenderos(idUsuario))
        val primerErrorDeSubida = subirPendientes(idUsuario)
        return if (primerErrorDeSubida == null) Result.success(Unit) else Result.failure(primerErrorDeSubida)
    }

    // Fusionar los documentos remotos con los locales aplicando que la última escritura prevalece
    private suspend fun fusionarRemotos(idUsuario: String, remotos: List<SenderoRed>) {
        remotos.forEach { remoto ->
            ejecutorTransacciones.ejecutar {
                val local = senderoDao.obtenerPorId(idUsuario, remoto.id)
                if (local == null || remoto.fechaModificacion > local.fechaModificacion) {
                    if (remoto.eliminado) {
                        senderoDao.borrarDefinitivamente(idUsuario, remoto.id)
                    } else {
                        senderoDao.guardar(remoto.aEntidad(idUsuario))
                    }
                }
            }
        }
    }

    // Subir los pendientes en hasta tres pasadas sin reintentar los fallidos; devolver el primer error o null
    private suspend fun subirPendientes(idUsuario: String): Throwable? {
        val idsFallidos = mutableSetOf<String>()
        var primerError: Throwable? = null
        repeat(MAXIMO_PASADAS_SUBIDA) {
            val pendientes = senderoDao.obtenerPendientes(idUsuario).filter { entidad -> entidad.id !in idsFallidos }
            if (pendientes.isEmpty()) return primerError
            pendientes.forEach { entidad ->
                val error = subirEntidad(idUsuario, entidad)
                if (error != null) {
                    idsFallidos.add(entidad.id)
                    if (primerError == null) primerError = error
                }
            }
        }
        return primerError
    }

    // Subir una entidad y transicionar su estado solo si no cambió durante la subida; devolver el error o null
    private suspend fun subirEntidad(idUsuario: String, entidad: SenderoEntidad): Throwable? {
        return try {
            val entidadConFotoRemota = subirFotoSiEsLocal(idUsuario, entidad)
            fuenteRemota.guardarSendero(idUsuario, entidadConFotoRemota.aRed())
            if (entidad.estaEliminado) {
                senderoDao.borrarEliminadoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion)
            } else {
                senderoDao.actualizarEstadoSiNoCambio(
                    idUsuario,
                    entidad.id,
                    entidad.fechaModificacion,
                    EstadoSincronizacion.SINCRONIZADO.name
                )
            }
            null
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo al subir el sendero ${entidad.id}", error)
            senderoDao.actualizarEstadoSiNoCambio(
                idUsuario,
                entidad.id,
                entidad.fechaModificacion,
                EstadoSincronizacion.CON_ERROR.name
            )
            error
        }
    }

    // Subir la foto local antes que el documento, porque Firestore solo debe guardar URLs que otros dispositivos puedan abrir
    private suspend fun subirFotoSiEsLocal(idUsuario: String, entidad: SenderoEntidad): SenderoEntidad {
        if (entidad.estaEliminado || !esFotoLocal(entidad.foto)) return entidad
        val urlRemota = subidorFotos.subir(entidad.foto)
        Log.d(TAG, "Foto del sendero ${entidad.id} subida a $urlRemota")
        senderoDao.actualizarFotoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion, urlRemota)
        return entidad.copy(foto = urlRemota)
    }
}
