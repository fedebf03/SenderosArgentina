package com.unaj.senderos.data.sincronizacion

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

private const val TAG = "SINCRONIZACION"

// Planificador que encola un trabajo único por usuario en WorkManager, exigiendo conexión
class PlanificadorSincronizacionWorkManager(
    private val workManager: WorkManager
) : PlanificadorSincronizacion {

    // Encolar el trabajo de sincronización conservando el ya encolado si existe
    override fun solicitarSincronizacion(idUsuario: String) {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val pedido = OneTimeWorkRequestBuilder<SincronizacionTrabajador>()
            .setConstraints(restricciones)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(SincronizacionTrabajador.CLAVE_ID_USUARIO to idUsuario))
            .build()
        Log.d(TAG, "Solicitando sincronización de senderos para el usuario $idUsuario")
        workManager.enqueueUniqueWork("sincronizar-senderos-$idUsuario", ExistingWorkPolicy.KEEP, pedido)
    }

    // Cancelar el trabajo único de sincronización del usuario
    override fun cancelarSincronizacion(idUsuario: String) {
        Log.d(TAG, "Cancelando sincronización de senderos para el usuario $idUsuario")
        workManager.cancelUniqueWork("sincronizar-senderos-$idUsuario")
    }
}
