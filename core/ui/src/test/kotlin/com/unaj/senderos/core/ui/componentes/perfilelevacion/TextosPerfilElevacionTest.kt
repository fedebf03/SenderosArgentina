package com.unaj.senderos.core.ui.componentes.perfilelevacion

import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.ui.comun.formatearAlturaMetros
import com.unaj.senderos.core.ui.comun.formatearPorcentaje
import org.junit.Assert.assertEquals
import org.junit.Test

class TextosPerfilElevacionTest {

    private val perfil = PerfilElevacion(
        puntos = listOf(PuntoPerfil(0.0, 1400.0), PuntoPerfil(5.0, 2100.0)),
        desnivelPositivoMetros = 700,
        desnivelNegativoMetros = 120,
        alturaMaximaMetros = 2100,
        alturaMinimaMetros = 1400,
        pendienteMaximaPorcentaje = 12.54,
        firmaTrazado = "firma"
    )

    @Test
    fun `el resumen accesible nombra la subida la bajada y la altura máxima`() {
        assertEquals(
            "Perfil de elevación: sube 700 m y baja 120 m, altura máxima 2.100 m",
            describirResumenPerfil(perfil)
        )
    }

    @Test
    fun `el punto tocado se describe con kilómetro y altura en formato argentino`() {
        assertEquals("Kilómetro 3,2 · altura 1.850 m", describirPuntoDelPerfil(3.24, 1850.4))
        assertEquals("Kilómetro 3 · altura 1.850 m", describirPuntoDelPerfil(3.0, 1850.0))
    }

    @Test
    fun `la posición actual agrega el estás acá`() {
        assertEquals("Estás acá: kilómetro 3,2 · altura 1.850 m", describirPosicionActual(3.24, 1850.4))
    }

    @Test
    fun `las alturas y pendientes usan separadores argentinos`() {
        assertEquals("2.100 m", formatearAlturaMetros(2100))
        assertEquals("950 m", formatearAlturaMetros(950))
        assertEquals("-105 m", formatearAlturaMetros(-105))
        assertEquals("12,5 %", formatearPorcentaje(12.54))
    }
}
