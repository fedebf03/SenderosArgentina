package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.falsos.FalsoDetectorCapacidades
import org.junit.Assert.assertEquals
import org.junit.Test

class ObtenerCapacidadesDispositivoUseCaseTest {

    // Verificar que el caso de uso devuelve lo que detecta el dispositivo
    @Test
    fun `devuelve las capacidades detectadas`() {
        val capacidades = CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = true, tieneBrujula = false, tieneCamara = true)
        val detector = FalsoDetectorCapacidades(capacidades)

        val resultado = ObtenerCapacidadesDispositivoUseCase(detector)()

        assertEquals(capacidades, resultado)
        assertEquals(1, detector.cantidadDeteccion)
    }
}
