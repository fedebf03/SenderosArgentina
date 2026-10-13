package com.unaj.senderos.data.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Display
import android.view.Surface
import com.unaj.senderos.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.domain.recorrido.CalculadoraRumbo
import com.unaj.senderos.domain.port.FuenteBrujula
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

private const val TAG = "SENSOR_BRUJULA"
private const val CAMBIO_MINIMO_GRADOS = 2f
private const val INCLINACION_VERTICAL_GRADOS = 45f
private const val TAMANO_MATRIZ = 9
private const val TAMANO_ORIENTACION = 3

// Entregar el azimut magnético desde TYPE_ROTATION_VECTOR (fusión de acelerómetro y magnetómetro del sistema), o desde
// TYPE_GEOMAGNETIC_ROTATION_VECTOR (sin giroscopio, menos preciso) si el equipo no tiene el primero.
// Frecuencia SENSOR_DELAY_UI (unos 16 Hz): alcanza para girar una flecha de forma fluida y gasta menos batería que GAME;
// además solo se emite cuando el rumbo cambió al menos 2 grados, para no recomponer la pantalla por ruido.
// Los eventos llegan en un hilo propio (HandlerThread) liberado al cancelar. El azimut es respecto del norte magnético
// (la corrección a norte verdadero la hace el control con la declinación de la última posición). Se remapean los ejes según
// la rotación de la pantalla y, si el equipo está de pie (inclinado más de 45 grados), para que el azimut sea el de la cámara trasera
class FuenteBrujulaAndroid(private val contexto: Context) : FuenteBrujula {

    // Observar el rumbo registrando el escucha al recolectar y quitándolo (junto con su hilo) al cancelar
    override fun observarRumbo(): Flow<Float> = callbackFlow {
        val gestor = contexto.getSystemService(SensorManager::class.java)
        val sensor = gestor?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: gestor?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        if (gestor == null || sensor == null) {
            close(IllegalStateException("El equipo no tiene sensor de rotación"))
            awaitClose { }
            return@callbackFlow
        }
        val gestorPantallas = contexto.getSystemService(DisplayManager::class.java)
        val hilo = HandlerThread("senderos-brujula").also { nuevoHilo -> nuevoHilo.start() }
        val matrizRotacion = FloatArray(TAMANO_MATRIZ)
        val matrizPantalla = FloatArray(TAMANO_MATRIZ)
        val matrizRemapeada = FloatArray(TAMANO_MATRIZ)
        val orientacion = FloatArray(TAMANO_ORIENTACION)
        var ultimoRumbo: Float? = null
        val escucha = object : SensorEventListener {

            // Calcular el azimut del evento y emitirlo si cambió lo suficiente respecto del último
            override fun onSensorChanged(evento: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrizRotacion, evento.values)
                val rotacionPantalla = gestorPantallas?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
                val rumbo = calcularAzimutGrados(matrizRotacion, rotacionPantalla, matrizPantalla, matrizRemapeada, orientacion)
                val anterior = ultimoRumbo
                if (anterior == null || CalculadoraRumbo.diferenciaAngularGrados(anterior, rumbo) >= CAMBIO_MINIMO_GRADOS) {
                    ultimoRumbo = rumbo
                    trySend(rumbo)
                }
            }

            // Ignorar los cambios de precisión: la app no distingue calibraciones
            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        val estaRegistrado = gestor.registerListener(escucha, sensor, SensorManager.SENSOR_DELAY_UI, Handler(hilo.looper))
        Log.d(TAG, "Brújula registrada: $estaRegistrado (sensor ${sensor.stringType})")
        if (!estaRegistrado) close(IllegalStateException("No se pudo registrar el sensor de rotación"))
        awaitClose {
            gestor.unregisterListener(escucha)
            hilo.quitSafely()
            Log.d(TAG, "Brújula liberada")
        }
    }

    // Calcular el azimut normalizado a [0, 360) remapeando los ejes por la rotación de la pantalla y, si está de pie, por la inclinación
    private fun calcularAzimutGrados(
        matrizRotacion: FloatArray,
        rotacionPantalla: Int,
        matrizPantalla: FloatArray,
        matrizRemapeada: FloatArray,
        orientacion: FloatArray
    ): Float {
        val (ejeX, ejeY) = when (rotacionPantalla) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrizRotacion, ejeX, ejeY, matrizPantalla)
        SensorManager.getOrientation(matrizPantalla, orientacion)
        val inclinacionGrados = Math.toDegrees(orientacion[1].toDouble()).toFloat()
        if (abs(inclinacionGrados) > INCLINACION_VERTICAL_GRADOS) {
            SensorManager.remapCoordinateSystem(matrizPantalla, SensorManager.AXIS_X, SensorManager.AXIS_Z, matrizRemapeada)
            SensorManager.getOrientation(matrizRemapeada, orientacion)
        }
        return CalculadoraRecorrido.normalizarRumbo(Math.toDegrees(orientacion[0].toDouble()).toFloat())
    }
}
