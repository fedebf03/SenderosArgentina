package com.unaj.senderos.core.data.seguimiento

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.unaj.senderos.core.domain.port.LanzadorSeguimiento
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "LANZADOR_SEGUIMIENTO"

class LanzadorSeguimientoAndroid @Inject constructor(@param:ApplicationContext private val contexto: Context) : LanzadorSeguimiento {

    override fun iniciar(idSendero: String, esSimulado: Boolean) {
        Log.d(TAG, "Solicitud de inicio del recorrido: $idSendero (simulado: $esSimulado)")
        ContextCompat.startForegroundService(
            contexto,
            ServicioSeguimientoRecorrido.crearIntencionIniciar(contexto, idSendero, esSimulado)
        )
    }

    override fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String) {
        Log.d(TAG, "Solicitud de inicio de una caminata libre")
        ContextCompat.startForegroundService(
            contexto,
            ServicioSeguimientoRecorrido.crearIntencionIniciarCaminataLibre(contexto, idCaminata, idUsuario, nombre)
        )
    }

    // Alcanza con detener el servicio, porque al destruirse también detiene el control del seguimiento
    override fun detener() {
        Log.d(TAG, "Solicitud de fin del recorrido")
        contexto.stopService(ServicioSeguimientoRecorrido.crearIntencionDetener(contexto))
    }

    // Si el sistema no deja iniciar el servicio es porque no está corriendo. Por lo tanto no hay recorrido que cancelar
    override fun cancelar() {
        Log.d(TAG, "Solicitud de cancelación del recorrido")
        try {
            contexto.startService(ServicioSeguimientoRecorrido.crearIntencionCancelar(contexto))
        } catch (error: IllegalStateException) {
            Log.e(TAG, "No se pudo pedir la cancelación: la app está en segundo plano y el servicio no corre", error)
        }
    }
}
