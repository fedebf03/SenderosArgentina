package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.repository.DetectorCapacidades

class ObtenerCapacidadesDispositivoUseCase(private val detector: DetectorCapacidades) {

    // Obtener las funciones disponibles en el equipo para informarlas al usuario
    operator fun invoke(): CapacidadesDispositivo = detector.detectar()
}
