package com.unaj.senderos.domain.recorrido

import com.unaj.senderos.domain.model.PuntoCardinal
import kotlin.math.abs

private const val GRADOS_POR_SECTOR = 45f
private const val MITAD_DE_SECTOR = 22.5f

object CalculadoraRumbo {

    // Convertir un rumbo en grados (0 = norte, sentido horario) al punto cardinal de su sector de 45 grados
    fun aPuntoCardinal(grados: Float): PuntoCardinal {
        val normalizados = CalculadoraRecorrido.normalizarRumbo(grados)
        val indice = ((normalizados + MITAD_DE_SECTOR) / GRADOS_POR_SECTOR).toInt() % PuntoCardinal.entries.size
        return PuntoCardinal.entries[indice]
    }

    // Calcular la diferencia angular absoluta entre dos rumbos por el camino más corto, entre 0 y 180 grados
    fun diferenciaAngularGrados(primero: Float, segundo: Float): Float {
        val diferencia = abs(CalculadoraRecorrido.normalizarRumbo(primero) - CalculadoraRecorrido.normalizarRumbo(segundo))
        return if (diferencia > 180f) 360f - diferencia else diferencia
    }
}
