package com.unaj.senderos.core.data.repository

import android.util.Log
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.SenderoDao
import com.unaj.senderos.core.database.SenderoEntidad
import com.unaj.senderos.core.data.mapeo.aDominio
import com.unaj.senderos.core.data.mapeo.aEntidad
import com.unaj.senderos.core.data.sincronizacion.PlanificadorSincronizacion
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
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

    override fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>> {
        return senderoDao.observarPorUsuario(idUsuario)
            .map { entidades -> entidades.map { entidad -> entidad.aDominio() } }
            .onStart { planificadorSincronizacion.solicitarSincronizacion(idUsuario) }
    }

    override fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?> {
        return senderoDao.observarPorId(idUsuario, idSendero)
            .map { entidad -> entidad?.aDominio() }
    }

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

    override suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit> {
        return modificarExistente(idUsuario, idSendero) { entidad ->
            entidad.copy(esFavorito = !entidad.esFavorito)
        }
    }

    override suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit> {
        return modificarExistente(idUsuario, idSendero) { entidad ->
            entidad.copy(estaEliminado = true)
        }
    }

    /**
     * Guardar el perfil de elevación calculado y autocompletar desnivel y alturas vacías.
     *
     * El perfil es un dato derivado del trazado, no una edición del usuario, por eso NO renueva `fechaModificacion`:
     * conserva la que ya tenía. Así no rompe el conteo de "guardados sin conexión" (RF7) ni gana la regla de la última
     * escritura contra una edición más nueva hecha en otro dispositivo. Solo pasa de SINCRONIZADO a PENDIENTE para que
     * el perfil se suba; un sendero PENDIENTE o CON_ERROR conserva su estado. Falla si el trazado cambió desde el cálculo.
     */
    override suspend fun guardarPerfilElevacion(idUsuario: String, idSendero: String, perfil: PerfilElevacion): Result<Unit> {
        return ejecutarEscritura(idUsuario) {
            ejecutorTransacciones.ejecutar {
                val entidad = senderoDao.obtenerPorId(idUsuario, idSendero)
                if (entidad == null || entidad.estaEliminado) {
                    throw NoSuchElementException("No se encontró el sendero")
                }
                val sendero = entidad.aDominio()
                if (CalculadoraPerfil.firmaTrazado(sendero.recorrido, sendero.tipoRecorrido) != perfil.firmaTrazado) {
                    throw IllegalStateException("El trazado cambió mientras se calculaba el perfil")
                }
                val estado = if (entidad.estadoSincronizacion == EstadoSincronizacion.SINCRONIZADO.name) {
                    EstadoSincronizacion.PENDIENTE.name
                } else {
                    entidad.estadoSincronizacion
                }
                senderoDao.guardar(
                    CalculadoraPerfil.aplicarAlSendero(sendero, perfil)
                        .aEntidad(entidad.fechaModificacion, EstadoSincronizacion.PENDIENTE, entidad.estaEliminado)
                        .copy(estadoSincronizacion = estado)
                )
            }
        }
    }

    override fun detenerSincronizacion(idUsuario: String) {
        Log.d(TAG, "Deteniendo la sincronización del usuario $idUsuario")
        planificadorSincronizacion.cancelarSincronizacion(idUsuario)
    }

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
