package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PuntoCardinal
import kotlin.math.abs

private const val GRADOS_POR_SECTOR = 45f
private const val MITAD_DE_SECTOR = 22.5f

object CalculadoraRumbo {

    // El 0 es el norte y se cuenta en sentido horario. Cada punto cardinal cubre un sector de 45 grados
    fun aPuntoCardinal(grados: Float): PuntoCardinal {
        val normalizados = CalculadoraRecorrido.normalizarRumbo(grados)
        val indice = ((normalizados + MITAD_DE_SECTOR) / GRADOS_POR_SECTOR).toInt() % PuntoCardinal.entries.size
        return PuntoCardinal.entries[indice]
    }

    fun diferenciaAngularGrados(primero: Float, segundo: Float): Float {
        val diferencia = abs(CalculadoraRecorrido.normalizarRumbo(primero) - CalculadoraRecorrido.normalizarRumbo(segundo))
        return if (diferencia > 180f) 360f - diferencia else diferencia
    }
}
