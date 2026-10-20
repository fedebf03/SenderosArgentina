package com.unaj.senderos.core.data.sensores

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.port.DetectorCapacidades
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "CAPACIDADES"

// La brújula cuenta como disponible si existe TYPE_ROTATION_VECTOR o, como respaldo, TYPE_GEOMAGNETIC_ROTATION_VECTOR.
// Son los sensores que usa la app, por lo tanto no alcanza con que haya magnetómetro
class DetectorCapacidadesAndroid @Inject constructor(@param:ApplicationContext private val contexto: Context) : DetectorCapacidades {

    override fun detectar(): CapacidadesDispositivo {
        val paquetes = contexto.packageManager
        val gestor = contexto.getSystemService(SensorManager::class.java)
        val capacidades = CapacidadesDispositivo(
            tieneGps = paquetes.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            tieneAcelerometro = gestor?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
            tieneBrujula = gestor?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                gestor?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null
        )
        Log.d(TAG, "Capacidades detectadas: $capacidades")
        return capacidades
    }
}
