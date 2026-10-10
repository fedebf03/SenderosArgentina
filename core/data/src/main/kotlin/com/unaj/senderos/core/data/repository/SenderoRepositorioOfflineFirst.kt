package com.unaj.senderos.data.repository

import android.util.Log
import com.unaj.senderos.data.local.EjecutorTransacciones
import com.unaj.senderos.data.local.SenderoDao
import com.unaj.senderos.data.local.SenderoEntidad
import com.unaj.senderos.data.mapeo.aDominio
import com.unaj.senderos.data.mapeo.aEntidad
import com.unaj.senderos.data.sincronizacion.PlanificadorSincronizacion
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "REPOSITORIO_SENDEROS"

// Repositorio offline-first: lee solo de Room y delega la red en la sincronización diferida
class SenderoRepositorioOfflineFirst(
    private val senderoDao: SenderoDao,
    private val planificadorSincronizacion: PlanificadorSincronizacion,
    private val ejecutorTransacciones: EjecutorTransacciones,
    private val reloj: () -> Long = System::currentTimeMillis
) : SenderoRepositorio {

    // Observar los senderos locales del usuario y pedir una sincronización al empezar a recolectar
    override fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>> {
        return senderoDao.observarPorUsuario(idUsuario)
            .map { entidades -> entidades.map { entidad -> entidad.aDominio() } }
            .onStart { planificadorSincronizacion.solicitarSincronizacion(idUsuario) }
    }

    // Observar un sendero local del usuario; emite null si no existe o fue eliminado
    override fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?> {
        return senderoDao.observarPorId(idUsuario, idSendero)
            .map { entidad -> entidad?.aDominio() }
    }

    // Guardar el sendero en Room como pendiente y solicitar la sincronización
    override suspend fun guardarSendero(idUsuario: String, sendero: Sendero): Result<Unit> {
        return ejecutarEscritura(idUsuario) {
            senderoDao.guardar(
                sendero.copy(idUsuario = idUsuario).aEntidad(
                    fechaModificacion = reloj(),
                    estadoSincronizacion = EstadoSincronizacion.PENDIENTE,
                    estaEliminado = false
                )
            )
        }
    }

    // Invertir la marca de favorito en Room como pendiente y solicitar la sincronización
    override suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit> {
        return modificarExistente(idUsuario, idSendero) { entidad ->
            entidad.copy(esFavorito = !entidad.esFavorito)
        }
    }

    // Marcar el sendero como eliminado en Room y solicitar la sincronización
    override suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit> {
        return modificarExistente(idUsuario, idSendero) { entidad ->
            entidad.copy(estaEliminado = true)
        }
    }

    // Detener la sincronización del usuario cancelando el trabajo planificado
    override fun detenerSincronizacion(idUsuario: String) {
        Log.d(TAG, "Deteniendo la sincronización del usuario $idUsuario")
        planificadorSincronizacion.cancelarSincronizacion(idUsuario)
    }

    // Aplicar una modificación a un sendero vigente dejándolo pendiente con la fecha actual
    private suspend fun modificarExistente(
        idUsuario: String,
        idSendero: String,
        modificar: (SenderoEntidad) -> SenderoEntidad
    ): Result<Unit> {
        return ejecutarEscritura(idUsuario) {
            ejecutorTransacciones.ejecutar {
                val entidad = senderoDao.obtenerPorId(idUsuario, idSendero)
                if (entidad == null || entidad.estaEliminado) {
                    throw NoSuchElementException("No se encontró el sendero")
                }
                senderoDao.guardar(
                    modificar(entidad).copy(
                        fechaModificacion = reloj(),
                        estadoSincronizacion = EstadoSincronizacion.PENDIENTE.name
                    )
                )
            }
        }
    }

    // Ejecutar una escritura local y, si sale bien, solicitar la sincronización del usuario
    private suspend fun ejecutarEscritura(idUsuario: String, escribir: suspend () -> Unit): Result<Unit> {
        return try {
            escribir()
            planificadorSincronizacion.solicitarSincronizacion(idUsuario)
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo la escritura local del usuario $idUsuario", error)
            Result.failure(error)
        }
    }
}
