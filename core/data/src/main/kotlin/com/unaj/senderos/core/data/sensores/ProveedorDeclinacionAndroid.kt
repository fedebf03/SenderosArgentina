package com.unaj.senderos.core.data.sensores

import android.hardware.GeomagneticField
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.ProveedorDeclinacion
import javax.inject.Inject

// La coordenada no trae altitud, por eso se usa 0 m (la altitud tiene un efecto despreciable sobre la declinación)
class ProveedorDeclinacionAndroid @Inject constructor() : ProveedorDeclinacion {

    override fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float {
        val campo = GeomagneticField(coordenada.latitud.toFloat(), coordenada.longitud.toFloat(), 0f, instanteMilisegundos)
        return campo.declination
    }
}
