package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.port.DetectorCapacidades

class ObtenerCapacidadesDispositivoUseCase(private val detector: DetectorCapacidades) {

    operator fun invoke(): CapacidadesDispositivo = detector.detectar()
}
