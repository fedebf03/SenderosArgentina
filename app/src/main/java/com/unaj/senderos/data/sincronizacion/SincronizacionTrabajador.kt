package com.unaj.senderos.data.sincronizacion

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.senderosargentina.SenderosAplicacion
import com.google.firebase.firestore.FirebaseFirestoreException

private const val TAG = "SINCRONIZACION"
private const val MAXIMO_REINTENTOS = 5

private val CODIGOS_REINTENTABLES = setOf(
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
    FirebaseFirestoreException.Code.ABORTED,
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
)

fun esErrorReintentable(error: Throwable?): Boolean {
    return if (error is FirebaseFirestoreException) error.code in CODIGOS_REINTENTABLES else true
}

class SincronizacionTrabajador(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val idUsuario = inputData.getString(CLAVE_ID_USUARIO).orEmpty()
        if (idUsuario.isBlank()) {
            Log.e(TAG, "Sincronización cancelada: falta el idUsuario")
            return Result.failure()
        }
        val aplicacion = applicationContext as SenderosAplicacion
        val resultado = aplicacion.contenedorDependencias.sincronizadorSenderos.sincronizar(idUsuario)
        if (resultado.isSuccess) {
            Log.d(TAG, "Sincronización terminada para el usuario $idUsuario")
            return Result.success()
        }
        val error = resultado.exceptionOrNull()
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

    companion object {
        const val CLAVE_ID_USUARIO = "idUsuario"
    }
}
