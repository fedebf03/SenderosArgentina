package com.unaj.senderos.core.data.sincronizacion

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unaj.senderos.core.domain.model.ResumenSincronizacion
import com.unaj.senderos.core.domain.notificaciones.DecisorNotificacionSincronizacion
import com.unaj.senderos.core.domain.notificaciones.MensajeNotificacion
import androidx.hilt.work.HiltWorker
import com.unaj.senderos.core.database.recorrido.RecorridoDao
import com.unaj.senderos.core.database.sendero.SenderoDao
import com.unaj.senderos.core.domain.port.Notificador
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.unaj.senderos.core.network.TipoErrorRemoto
import com.unaj.senderos.core.network.tipoErrorRemoto

private const val TAG = "SINCRONIZACION"
private const val MAXIMO_REINTENTOS = 5

private val ERRORES_REINTENTABLES = setOf(
    TipoErrorRemoto.SIN_RESPUESTA,
    TipoErrorRemoto.TRANSITORIO,
    TipoErrorRemoto.DESCONOCIDO
)

// Los errores permanentes del servidor no se reintentan porque volverían a fallar de la misma forma
fun esErrorReintentable(error: Throwable?): Boolean {
    return error.tipoErrorRemoto() in ERRORES_REINTENTABLES
}

@HiltWorker
class SincronizacionTrabajador @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val sincronizadorSenderos: SincronizadorSenderos,
    private val sincronizadorRecorridos: SincronizadorRecorridos,
    private val notificador: Notificador,
    private val senderoDao: SenderoDao,
    private val recorridoDao: RecorridoDao
) : CoroutineWorker(contexto, parametros) {

    // RF7 - Sincronización: el worker sube y baja senderos y recorridos, y decide si reintentar o avisar del error
    override suspend fun doWork(): Result {
        val idUsuario = inputData.getString(CLAVE_ID_USUARIO).orEmpty()
        if (idUsuario.isBlank()) {
            Log.e(TAG, "Sincronización cancelada: falta el idUsuario")
            return Result.failure()
        }
        val resultado = sincronizadorSenderos.sincronizar(idUsuario)
        val errorRecorridos = sincronizadorRecorridos.sincronizar(idUsuario).exceptionOrNull()
        val primerError = resultado.getOrNull()?.primerError ?: errorRecorridos
        if (resultado.isSuccess && primerError == null) {
            Log.d(TAG, "Sincronización terminada para el usuario $idUsuario")
            resultado.getOrNull()?.let { resumen -> notificarResultado(resumen, true) }
            notificador.cancelarAvisoDeError()
            return Result.success()
        }
        val error = resultado.exceptionOrNull() ?: primerError
        val noSeReintentara = runAttemptCount >= MAXIMO_REINTENTOS || !esErrorReintentable(error)
        val resumen = resultado.getOrNull()
        if (resumen != null) {
            notificarResultado(resumen, noSeReintentara)
        } else if (noSeReintentara) {
            notificarPendientesSinSincronizar(idUsuario)
        }
        return when {
            runAttemptCount >= MAXIMO_REINTENTOS -> {
                Log.e(TAG, "Sincronización abandonada tras $runAttemptCount intentos", error)
                Result.failure()
            }
            !esErrorReintentable(error) -> {
                Log.e(TAG, "Sincronización fallida con error permanente, no se reintentará", error)
                Result.failure()
            }
            else -> {
                Log.e(TAG, "Sincronización fallida, se reintentará", error)
                Result.retry()
            }
        }
    }

    private fun notificarResultado(
        resumen: ResumenSincronizacion,
        esUltimoIntento: Boolean
    ) {
        val mensaje = DecisorNotificacionSincronizacion.decidir(resumen, esUltimoIntento) ?: return
        notificador.notificar(mensaje)
    }

    private suspend fun notificarPendientesSinSincronizar(idUsuario: String) {
        val cantidadPendientes = senderoDao.obtenerPendientes(idUsuario).size +
            recorridoDao.obtenerPendientes(idUsuario).size
        if (cantidadPendientes > 0) {
            notificador.notificar(
                MensajeNotificacion.SenderosConError(cantidadPendientes)
            )
        }
    }

    companion object {
        const val CLAVE_ID_USUARIO = "idUsuario"
    }
}
