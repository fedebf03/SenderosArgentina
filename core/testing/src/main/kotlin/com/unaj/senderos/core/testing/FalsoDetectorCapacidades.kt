package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.port.DetectorCapacidades

class FalsoDetectorCapacidades(var capacidades: CapacidadesDispositivo) : DetectorCapacidades {

    var cantidadDeteccion: Int = 0
        private set

    override fun detectar(): CapacidadesDispositivo {
        cantidadDeteccion++
        return capacidades
    }
}
