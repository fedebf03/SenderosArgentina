package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.repository.DetectorCapacidades

class FalsoDetectorCapacidades(var capacidades: CapacidadesDispositivo) : DetectorCapacidades {

    var cantidadDeteccion: Int = 0
        private set

    // Contar la consulta y devolver las capacidades configuradas
    override fun detectar(): CapacidadesDispositivo {
        cantidadDeteccion++
        return capacidades
    }
}
