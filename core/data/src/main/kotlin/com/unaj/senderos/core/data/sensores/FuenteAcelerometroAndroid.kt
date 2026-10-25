package com.unaj.senderos.core.data.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import com.unaj.senderos.core.domain.model.MuestraAcelerometro
import com.unaj.senderos.core.domain.port.FuenteAcelerometro
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicInteger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "SENSOR_PASOS"
private const val NANOSEGUNDOS_POR_MILISEGUNDO = 1_000_000L
private const val CAPACIDAD_MUESTRAS = 1024
private const val LATENCIA_LOTE_MICROSEGUNDOS = 2_000_000
private const val SIN_LOTE_MICROSEGUNDOS = 0

// RF6 - Pasos: usamos el acelerómetro y no TYPE_STEP_COUNTER porque el algoritmo de pasos es propio. Además el contador
// del sistema pide el permiso de actividad física y no existe en todos los equipos
class FuenteAcelerometroAndroid @Inject constructor(@param:ApplicationContext private val contexto: Context) : FuenteAcelerometro {

    override fun observarMuestras(): Flow<MuestraAcelerometro> = callbackFlow {
        val gestor = contexto.getSystemService(SensorManager::class.java)
        val sensor = gestor?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (gestor == null || sensor == null) {
            close(IllegalStateException("El equipo no tiene acelerómetro"))
            awaitClose { }
            return@callbackFlow
        }
        // Las lecturas se reciben en un hilo aparte para no perderlas si el hilo principal está ocupado
        val hilo = HandlerThread("senderos-acelerometro").also { nuevoHilo -> nuevoHilo.start() }
        val descartadas = AtomicInteger(0)
        val escucha = object : SensorEventListener {

            override fun onSensorChanged(evento: SensorEvent) {
                // El sensor cuenta el tiempo desde que se encendió el equipo. Para pasarlo a la hora real se suma la diferencia actual
                val desfaseMilisegundos = System.currentTimeMillis() - SystemClock.elapsedRealtime()
                val instante = evento.timestamp / NANOSEGUNDOS_POR_MILISEGUNDO + desfaseMilisegundos
                val valores = evento.values
                val resultado = trySend(MuestraAcelerometro(valores[0], valores[1], valores[2], instante))
                if (resultado.isFailure && descartadas.incrementAndGet() % AVISO_CADA_DESCARTADAS == 1) {
                    Log.w(TAG, "Muestras descartadas por búfer lleno: ${descartadas.get()}")
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        // SENSOR_DELAY_GAME (unos 50 Hz) da al menos 8 muestras por paso. En cambio UI (unos 16 Hz) dejaría 5 al trotar.
        // Si el sensor puede acumular lecturas (FIFO), se piden juntas cada 2 s como máximo y así la CPU se despierta menos
        val latencia = if (sensor.fifoMaxEventCount > 0) LATENCIA_LOTE_MICROSEGUNDOS else SIN_LOTE_MICROSEGUNDOS
        val estaRegistrado = gestor.registerListener(escucha, sensor, SensorManager.SENSOR_DELAY_GAME, latencia, Handler(hilo.looper))
        Log.d(TAG, "Acelerómetro registrado: $estaRegistrado (latencia de lote: $latencia us)")
        if (!estaRegistrado) close(IllegalStateException("No se pudo registrar el acelerómetro"))
        awaitClose {
            gestor.unregisterListener(escucha)
            hilo.quitSafely()
            Log.d(TAG, "Acelerómetro liberado; muestras descartadas: ${descartadas.get()}")
        }
    }.buffer(CAPACIDAD_MUESTRAS)

    private companion object {
        const val AVISO_CADA_DESCARTADAS = 100
    }
}
