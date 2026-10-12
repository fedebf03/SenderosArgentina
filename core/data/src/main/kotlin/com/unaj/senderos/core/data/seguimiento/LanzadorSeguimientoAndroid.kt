package com.unaj.senderos.data.seguimiento

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.unaj.senderos.domain.repository.LanzadorSeguimiento

private const val TAG = "LANZADOR_SEGUIMIENTO"

// Lanzar y detener el servicio en primer plano del recorrido sin exponer intents a la presentación
class LanzadorSeguimientoAndroid(private val contexto: Context) : LanzadorSeguimiento {

    // Iniciar el servicio en primer plano para el sendero indicado
    override fun iniciar(idSendero: String, esSimulado: Boolean) {
        Log.d(TAG, "Solicitud de inicio del recorrido: $idSendero (simulado: $esSimulado)")
        ContextCompat.startForegroundService(
            contexto,
            ServicioSeguimientoRecorrido.crearIntencionIniciar(contexto, idSendero, esSimulado)
        )
    }

    // Detener el servicio; el servicio detiene el control al destruirse
    override fun detener() {
        Log.d(TAG, "Solicitud de fin del recorrido")
        contexto.stopService(ServicioSeguimientoRecorrido.crearIntencionDetener(contexto))
    }
}
