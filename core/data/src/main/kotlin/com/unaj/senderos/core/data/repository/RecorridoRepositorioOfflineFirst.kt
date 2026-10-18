package com.unaj.senderos.core.data.repository

import android.util.Log
import com.unaj.senderos.core.database.RecorridoDao
import com.unaj.senderos.core.data.mapeo.aDominio
import com.unaj.senderos.core.data.mapeo.aEntidad
import com.unaj.senderos.core.data.sincronizacion.PlanificadorSincronizacion
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "REPOSITORIO_RECORRIDOS"

class RecorridoRepositorioOfflineFirst(
    private val recorridoDao: RecorridoDao,
    private val planificadorSincronizacion: PlanificadorSincronizacion,
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val reloj: () -> Long = System::currentTimeMillis
) : RecorridoRepositorio {

    // Pide la sincronización al empezar a recolectar
    override fun observarRecorridos(idUsuario: String): Flow<List<RecorridoRealizado>> {
        return recorridoDao.observarPorUsuario(idUsuario)
            .map { entidades -> entidades.map { entidad -> entidad.aDominio() } }
            .onStart { planificadorSincronizacion.solicitarSincronizacion(idUsuario) }
    }

    override fun observarRecorrido(idUsuario: String, idRecorrido: String): Flow<RecorridoRealizado?> {
        return recorridoDao.observarPorId(idUsuario, idRecorrido)
            .map { entidad -> entidad?.aDominio() }
    }

    // Marcar el recorrido como eliminado y pendiente con la fecha actual y pedir la sincronización, que borra el documento de Firestore y recién
    // entonces la fila local. Sin la sesión del dueño queda marcado y se limpia en su próximo ingreso
    override suspend fun eliminar(idUsuario: String, idRecorrido: String): Result<Unit> {
        return try {
            val existente = recorridoDao.obtenerPorId(idUsuario, idRecorrido)
            if (existente == null || existente.estaEliminado) return Result.success(Unit)
            recorridoDao.guardar(
                existente.copy(
                    estaEliminado = true,
                    estadoSincronizacion = EstadoSincronizacion.PENDIENTE.name,
                    fechaModificacion = reloj()
                )
            )
            if (autenticacionRepositorio.obtenerUsuarioActual()?.id == idUsuario) {
                planificadorSincronizacion.solicitarSincronizacion(idUsuario)
            }
            Log.d(TAG, "Recorrido $idRecorrido marcado como eliminado")
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo la eliminación local del recorrido $idRecorrido", error)
            Result.failure(error)
        }
    }

    // Guardar en Room como pendiente con la fecha del reloj y pedir la sincronización solo si la escritura salió bien y el dueño tiene la sesión activa
    override suspend fun guardar(recorrido: RecorridoRealizado): Result<Unit> {
        return try {
            recorridoDao.guardar(
                recorrido.aEntidad(fechaModificacion = reloj(), estadoSincronizacion = EstadoSincronizacion.PENDIENTE)
            )
            if (autenticacionRepositorio.obtenerUsuarioActual()?.id == recorrido.idUsuario) {
                planificadorSincronizacion.solicitarSincronizacion(recorrido.idUsuario)
            } else {
                Log.d(TAG, "Sin sesión del dueño: el recorrido ${recorrido.id} se subirá en su próximo ingreso")
            }
            Log.d(TAG, "Recorrido ${recorrido.id} guardado como pendiente")
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo la escritura local del recorrido ${recorrido.id}", error)
            Result.failure(error)
        }
    }
}
