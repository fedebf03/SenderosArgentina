package com.unaj.senderos.data.sensores

import android.hardware.GeomagneticField
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.port.ProveedorDeclinacion

// La coordenada no trae altitud, por eso se usa 0 m (la altitud tiene un efecto despreciable sobre la declinación)
class ProveedorDeclinacionAndroid : ProveedorDeclinacion {

    override fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float {
        val campo = GeomagneticField(coordenada.latitud.toFloat(), coordenada.longitud.toFloat(), 0f, instanteMilisegundos)
        return campo.declination
    }
}
