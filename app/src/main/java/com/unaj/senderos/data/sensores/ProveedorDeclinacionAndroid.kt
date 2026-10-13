package com.unaj.senderos.data.sensores

import android.hardware.GeomagneticField
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.port.ProveedorDeclinacion

// Calcular la declinación magnética con el modelo geomagnético del sistema (GeomagneticField); la coordenada no trae altitud,
// por eso se usa 0 m (la altitud tiene un efecto despreciable sobre la declinación)
class ProveedorDeclinacionAndroid : ProveedorDeclinacion {

    // Obtener la declinación en grados (positiva al este) de la coordenada en el instante indicado
    override fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float {
        val campo = GeomagneticField(coordenada.latitud.toFloat(), coordenada.longitud.toFloat(), 0f, instanteMilisegundos)
        return campo.declination
    }
}
