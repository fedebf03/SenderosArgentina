package com.unaj.senderos.domain.port

import com.unaj.senderos.domain.model.CapacidadesDispositivo

interface DetectorCapacidades {

    fun detectar(): CapacidadesDispositivo
}
