package com.unaj.senderos.core.data.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.unaj.senderos.core.domain.port.FuentePresion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

private const val TAG = "SENSOR_PRESION"

// SENSOR_DELAY_NORMAL alcanza para seguir una tendencia de presión de varias horas.
// Los eventos llegan en un hilo propio que se libera al cancelar
class FuentePresionAndroid @Inject constructor(@param:ApplicationContext private val contexto: Context) : FuentePresion {

    override fun observarPresion(): Flow<Double> = callbackFlow {
        val gestor = contexto.getSystemService(SensorManager::class.java)
        val sensor = gestor?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (gestor == null || sensor == null) {
            close(IllegalStateException("El equipo no tiene barómetro"))
            awaitClose { }
            return@callbackFlow
        }
        val hilo = HandlerThread("senderos-presion").also { nuevoHilo -> nuevoHilo.start() }
        val escucha = object : SensorEventListener {

            override fun onSensorChanged(evento: SensorEvent) {
                trySend(evento.values[0].toDouble())
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        val estaRegistrado = gestor.registerListener(escucha, sensor, SensorManager.SENSOR_DELAY_NORMAL, Handler(hilo.looper))
        Log.d(TAG, "Barómetro registrado: $estaRegistrado")
        if (!estaRegistrado) close(IllegalStateException("No se pudo registrar el barómetro"))
        awaitClose {
            gestor.unregisterListener(escucha)
            hilo.quitSafely()
            Log.d(TAG, "Barómetro liberado")
        }
    }
}
