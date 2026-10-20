package com.unaj.senderos.core.data.sincronizacion

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion
import com.unaj.senderos.core.domain.port.SolicitudSincronizacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "SINCRONIZACION"

class SolicitudSincronizacionWorkManager @Inject constructor(
    private val workManager: WorkManager
) : SolicitudSincronizacion {

    // Se reemplaza el trabajo pendiente. Si estaba esperando un reintento por un fallo, se vuelve a intentar en el momento
    override fun sincronizarAhora(idUsuario: String) {
        val restricciones = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val pedido = OneTimeWorkRequestBuilder<SincronizacionTrabajador>()
            .setConstraints(restricciones)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(SincronizacionTrabajador.CLAVE_ID_USUARIO to idUsuario))
            .build()
        Log.d(TAG, "Sincronización manual pedida para el usuario $idUsuario")
        workManager.enqueueUniqueWork(nombreDelTrabajo(idUsuario), ExistingWorkPolicy.REPLACE, pedido)
    }

    override fun observarEstado(idUsuario: String): Flow<EstadoTrabajoSincronizacion> =
        workManager.getWorkInfosForUniqueWorkFlow(nombreDelTrabajo(idUsuario)).map { trabajos ->
            when {
                trabajos.any { trabajo -> trabajo.state == WorkInfo.State.RUNNING } -> EstadoTrabajoSincronizacion.EN_CURSO
                trabajos.any { trabajo -> trabajo.state == WorkInfo.State.ENQUEUED && trabajo.runAttemptCount > 0 } -> EstadoTrabajoSincronizacion.EN_REINTENTO
                trabajos.any { trabajo -> trabajo.state == WorkInfo.State.ENQUEUED || trabajo.state == WorkInfo.State.BLOCKED } -> EstadoTrabajoSincronizacion.EN_COLA
                else -> EstadoTrabajoSincronizacion.INACTIVO
            }
        }

    private fun nombreDelTrabajo(idUsuario: String) = "sincronizar-senderos-$idUsuario"
}
