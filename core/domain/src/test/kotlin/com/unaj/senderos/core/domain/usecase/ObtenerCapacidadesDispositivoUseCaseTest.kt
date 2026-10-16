package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.testing.FalsoDetectorCapacidades
import org.junit.Assert.assertEquals
import org.junit.Test

class ObtenerCapacidadesDispositivoUseCaseTest {

    @Test
    fun `devuelve las capacidades detectadas`() {
        val capacidades = CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = true, tieneBrujula = false)
        val detector = FalsoDetectorCapacidades(capacidades)

        val resultado = ObtenerCapacidadesDispositivoUseCase(detector)()

        assertEquals(capacidades, resultado)
        assertEquals(1, detector.cantidadDeteccion)
    }
}
