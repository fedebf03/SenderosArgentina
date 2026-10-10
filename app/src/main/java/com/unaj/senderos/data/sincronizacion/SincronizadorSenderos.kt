package com.unaj.senderos.data.sincronizacion

import android.util.Log
import com.unaj.senderos.data.ejemplos.crearSenderosDeEjemplo
import com.unaj.senderos.data.local.EjecutorTransacciones
import com.unaj.senderos.data.local.SenderoDao
import com.unaj.senderos.data.local.SenderoEntidad
import com.unaj.senderos.data.mapeo.aEntidad
import com.unaj.senderos.data.mapeo.aRed
import com.unaj.senderos.data.remote.SenderoFuenteRemota
import com.unaj.senderos.data.remote.SenderoRed
import com.unaj.senderos.domain.model.EstadoSincronizacion
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SINCRONIZACION"
private const val MAXIMO_PASADAS_SUBIDA = 3

// Sincronizar la base local (fuente de verdad) con Firestore: pull, fusión por fecha y subida diferida
class SincronizadorSenderos(
    private val senderoDao: SenderoDao,
    private val fuenteRemota: SenderoFuenteRemota,
    private val ejecutorTransacciones: EjecutorTransacciones,
    private val reloj: () -> Long = System::currentTimeMillis
) {
    // Ejecutar el ciclo completo de sincronización de un usuario devolviendo el resultado
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

    // Recorrer los cinco pasos de la sincronización dejando propagar las excepciones
    private suspend fun sincronizarSinCapturar(idUsuario: String): Result<Unit> {
        val remotos = fuenteRemota.obtenerSenderos(idUsuario)
        val marcaEjemplosCargados = fuenteRemota.obtenerMarcaEjemplosCargados(idUsuario)

        if (!marcaEjemplosCargados && remotos.isEmpty() && senderoDao.obtenerTodosDelUsuario(idUsuario).isEmpty()) {
            sembrarEjemplos(idUsuario)
        }

        fusionarRemotos(idUsuario, remotos)

        val primerErrorDeSubida = subirPendientes(idUsuario)
        if (primerErrorDeSubida != null) {
            return Result.failure(primerErrorDeSubida)
        }

        if (!marcaEjemplosCargados) {
            fuenteRemota.registrarMarcaEjemplosCargados(idUsuario)
        }
        return Result.success(Unit)
    }

    // Guardar los senderos de ejemplo como pendientes para que se suban en este mismo ciclo
    private suspend fun sembrarEjemplos(idUsuario: String) {
        crearSenderosDeEjemplo(idUsuario).forEach { sendero ->
            senderoDao.guardar(
                sendero.aEntidad(
                    fechaModificacion = reloj(),
                    estadoSincronizacion = EstadoSincronizacion.PENDIENTE,
                    estaEliminado = false
                )
            )
        }
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
            fuenteRemota.guardarSendero(idUsuario, entidad.aRed())
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
}
