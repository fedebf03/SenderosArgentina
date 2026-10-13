package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.port.DetectorCapacidades

class FalsoDetectorCapacidades(var capacidades: CapacidadesDispositivo) : DetectorCapacidades {

    var cantidadDeteccion: Int = 0
        private set

    override fun detectar(): CapacidadesDispositivo {
        cantidadDeteccion++
        return capacidades
    }
}
