package com.unaj.senderos.data.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import com.unaj.senderos.domain.model.MuestraAcelerometro
import com.unaj.senderos.domain.port.FuenteAcelerometro
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "SENSOR_PASOS"
private const val NANOSEGUNDOS_POR_MILISEGUNDO = 1_000_000L
private const val CAPACIDAD_MUESTRAS = 1024
private const val LATENCIA_LOTE_MICROSEGUNDOS = 2_000_000
private const val SIN_LOTE_MICROSEGUNDOS = 0

// Entregar las muestras de TYPE_ACCELEROMETER; se eligió el acelerómetro (y no TYPE_STEP_COUNTER) porque el algoritmo de pasos es propio y
// porque el contador de pasos del sistema exige el permiso de reconocimiento de actividad y no existe en todos los equipos.
// Frecuencia SENSOR_DELAY_GAME (unos 50 Hz): a 2-3 pasos por segundo la detección de picos necesita al menos 8 muestras por paso;
// con SENSOR_DELAY_UI (unos 16 Hz) un trote quedaría con 5 muestras por paso. El acelerómetro es de bajo consumo.
// Si el sensor tiene FIFO (fifoMaxEventCount > 0) se piden lotes de hasta 2 s para despertar menos la CPU; las muestras conservan su instante real.
// Las muestras llegan en un hilo propio (HandlerThread) para no perderlas si el hilo principal está ocupado.
// El instante se expresa en reloj de pared: instante del evento del sensor (monotónico) más el desfase actual entre ambos relojes
class FuenteAcelerometroAndroid(private val contexto: Context) : FuenteAcelerometro {

    override fun observarMuestras(): Flow<MuestraAcelerometro> = callbackFlow {
        val gestor = contexto.getSystemService(SensorManager::class.java)
        val sensor = gestor?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (gestor == null || sensor == null) {
            close(IllegalStateException("El equipo no tiene acelerómetro"))
            awaitClose { }
            return@callbackFlow
        }
        val hilo = HandlerThread("senderos-acelerometro").also { nuevoHilo -> nuevoHilo.start() }
        val descartadas = AtomicInteger(0)
        val escucha = object : SensorEventListener {

            override fun onSensorChanged(evento: SensorEvent) {
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
