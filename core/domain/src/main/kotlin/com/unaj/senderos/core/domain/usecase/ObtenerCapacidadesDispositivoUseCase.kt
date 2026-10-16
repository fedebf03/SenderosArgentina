package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.port.DetectorCapacidades
import javax.inject.Inject

class ObtenerCapacidadesDispositivoUseCase @Inject constructor(private val detector: DetectorCapacidades) {

    operator fun invoke(): CapacidadesDispositivo = detector.detectar()
}
