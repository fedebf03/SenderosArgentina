package com.unaj.senderos.data.sensores

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.port.DetectorCapacidades

private const val TAG = "CAPACIDADES"

// La brújula se considera disponible si existe TYPE_ROTATION_VECTOR o, como respaldo, TYPE_GEOMAGNETIC_ROTATION_VECTOR (los sensores que realmente usa la app), no solo el magnetómetro
class DetectorCapacidadesAndroid(private val contexto: Context) : DetectorCapacidades {

    override fun detectar(): CapacidadesDispositivo {
        val paquetes = contexto.packageManager
        val gestor = contexto.getSystemService(SensorManager::class.java)
        val capacidades = CapacidadesDispositivo(
            tieneGps = paquetes.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            tieneAcelerometro = gestor?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
            tieneBrujula = gestor?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                gestor?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null,
            tieneCamara = paquetes.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        )
        Log.d(TAG, "Capacidades detectadas: $capacidades")
        return capacidades
    }
}
