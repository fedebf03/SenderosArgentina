package com.unaj.senderos.data.sincronizacion

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.senderosargentina.SenderosAplicacion
import com.google.firebase.firestore.FirebaseFirestoreException
import com.unaj.senderos.domain.DecisorNotificacionSincronizacion
import com.unaj.senderos.domain.model.MensajeNotificacion
import com.unaj.senderos.domain.model.ResumenSincronizacion

private const val TAG = "SINCRONIZACION"
private const val MAXIMO_REINTENTOS = 5

private val CODIGOS_REINTENTABLES = setOf(
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
    FirebaseFirestoreException.Code.ABORTED,
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
)

// Determinar si un error merece reintento: los códigos permanentes de Firestore no lo merecen
fun esErrorReintentable(error: Throwable?): Boolean {
    return if (error is FirebaseFirestoreException) error.code in CODIGOS_REINTENTABLES else true
}

class SincronizacionTrabajador(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    // Sincronizar los senderos del usuario, notificar el resultado y decidir si WorkManager reintenta
    override suspend fun doWork(): Result {
        val idUsuario = inputData.getString(CLAVE_ID_USUARIO).orEmpty()
        if (idUsuario.isBlank()) {
            Log.e(TAG, "Sincronización cancelada: falta el idUsuario")
            return Result.failure()
        }
        val aplicacion = applicationContext as SenderosAplicacion
        val resultado = aplicacion.contenedorDependencias.sincronizadorSenderos.sincronizar(idUsuario)
        val primerError = resultado.getOrNull()?.primerError
        if (resultado.isSuccess && primerError == null) {
            Log.d(TAG, "Sincronización terminada para el usuario $idUsuario")
            resultado.getOrNull()?.let { resumen -> notificarResultado(aplicacion, resumen, true) }
            aplicacion.contenedorDependencias.notificador.cancelarAvisoDeError()
            return Result.success()
        }
        val error = resultado.exceptionOrNull() ?: primerError
        val noSeReintentara = runAttemptCount >= MAXIMO_REINTENTOS || !esErrorReintentable(error)
        val resumen = resultado.getOrNull()
        if (resumen != null) {
            notificarResultado(aplicacion, resumen, noSeReintentara)
        } else if (noSeReintentara) {
            notificarPendientesSinSincronizar(aplicacion, idUsuario)
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

    // Notificar al usuario el resultado de la sincronización cuando el decisor lo indique
    private fun notificarResultado(
        aplicacion: SenderosAplicacion,
        resumen: ResumenSincronizacion,
        esUltimoIntento: Boolean
    ) {
        val mensaje = DecisorNotificacionSincronizacion.decidir(resumen, esUltimoIntento) ?: return
        aplicacion.contenedorDependencias.notificador.notificar(mensaje)
    }

    // Notificar el error con la cantidad de pendientes cuando la sincronización falló por completo y no se reintentará
    private suspend fun notificarPendientesSinSincronizar(aplicacion: SenderosAplicacion, idUsuario: String) {
        val cantidadPendientes = aplicacion.contenedorDependencias.senderoDao.obtenerPendientes(idUsuario).size
        if (cantidadPendientes > 0) {
            aplicacion.contenedorDependencias.notificador.notificar(
                MensajeNotificacion.SenderosConError(cantidadPendientes)
            )
        }
    }

    companion object {
        const val CLAVE_ID_USUARIO = "idUsuario"
    }
}
