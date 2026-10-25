package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.CapacidadesDispositivo

interface DetectorCapacidades {

    fun detectar(): CapacidadesDispositivo
}
