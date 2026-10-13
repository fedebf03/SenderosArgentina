package com.unaj.senderos.data.seguimiento

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.unaj.senderos.domain.port.LanzadorSeguimiento

private const val TAG = "LANZADOR_SEGUIMIENTO"

class LanzadorSeguimientoAndroid(private val contexto: Context) : LanzadorSeguimiento {

    override fun iniciar(idSendero: String, esSimulado: Boolean) {
        Log.d(TAG, "Solicitud de inicio del recorrido: $idSendero (simulado: $esSimulado)")
        ContextCompat.startForegroundService(
            contexto,
            ServicioSeguimientoRecorrido.crearIntencionIniciar(contexto, idSendero, esSimulado)
        )
    }

    // El servicio detiene el control al destruirse
    override fun detener() {
        Log.d(TAG, "Solicitud de fin del recorrido")
        contexto.stopService(ServicioSeguimientoRecorrido.crearIntencionDetener(contexto))
    }

    // Si el sistema no permite iniciar el servicio no hay recorrido que cancelar
    override fun cancelar() {
        Log.d(TAG, "Solicitud de cancelación del recorrido")
        try {
            contexto.startService(ServicioSeguimientoRecorrido.crearIntencionCancelar(contexto))
        } catch (error: IllegalStateException) {
            Log.e(TAG, "No se pudo pedir la cancelación: la app está en segundo plano y el servicio no corre", error)
        }
    }
}
