package com.unaj.senderos.core.data.sensores

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
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraRumbo
import com.unaj.senderos.core.domain.port.FuenteBrujula
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "SENSOR_BRUJULA"
private const val CAMBIO_MINIMO_GRADOS = 2f
private const val INCLINACION_VERTICAL_GRADOS = 45f
private const val TAMANO_MATRIZ = 9
private const val TAMANO_ORIENTACION = 3

// RF6 - Brújula: el rumbo sale respecto del norte magnético y el control lo corrige con la declinación de la posición
class FuenteBrujulaAndroid @Inject constructor(@param:ApplicationContext private val contexto: Context) : FuenteBrujula {

    override fun observarRumbo(): Flow<Float> = callbackFlow {
        val gestor = contexto.getSystemService(SensorManager::class.java)
        // TYPE_GEOMAGNETIC_ROTATION_VECTOR es el respaldo para equipos sin giroscopio (es menos preciso)
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

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        // SENSOR_DELAY_UI (unos 16 Hz) alcanza para girar la flecha y gasta menos batería que GAME. Además el rumbo solo
        // se informa si cambió 2 grados o más, para no redibujar la pantalla por pequeñas variaciones del sensor
        val estaRegistrado = gestor.registerListener(escucha, sensor, SensorManager.SENSOR_DELAY_UI, Handler(hilo.looper))
        Log.d(TAG, "Brújula registrada: $estaRegistrado (sensor ${sensor.stringType})")
        if (!estaRegistrado) close(IllegalStateException("No se pudo registrar el sensor de rotación"))
        awaitClose {
            gestor.unregisterListener(escucha)
            hilo.quitSafely()
            Log.d(TAG, "Brújula liberada")
        }
    }

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
        // Con el equipo de pie (más de 45 grados) se cambian los ejes de referencia para que el rumbo sea el de la cámara trasera
        if (abs(inclinacionGrados) > INCLINACION_VERTICAL_GRADOS) {
            SensorManager.remapCoordinateSystem(matrizPantalla, SensorManager.AXIS_X, SensorManager.AXIS_Z, matrizRemapeada)
            SensorManager.getOrientation(matrizRemapeada, orientacion)
        }
        return CalculadoraRecorrido.normalizarRumbo(Math.toDegrees(orientacion[0].toDouble()).toFloat())
    }
}
