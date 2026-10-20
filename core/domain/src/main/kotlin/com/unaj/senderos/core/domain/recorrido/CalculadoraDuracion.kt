package com.unaj.senderos.core.domain.recorrido

import kotlin.math.roundToInt

private const val KILOMETROS_POR_HORA_EN_LLANO = 4.0
private const val METROS_DE_SUBIDA_POR_HORA = 600.0
private const val MINUTOS_POR_HORA = 60.0
private const val REDONDEO_MINUTOS = 5
// Un mes de caminata. Más que eso es un dato mal cargado y además desbordaría el entero
private const val MAXIMO_MINUTOS = 60.0 * 24 * 30

object CalculadoraDuracion {

    // Usamos la regla de Naismith (4 km por hora en llano más una hora cada 600 m de subida), redondeada a 5 minutos.
    // Es una referencia para alguien en buen estado y no cuenta los descansos
    fun estimarMinutos(distanciaKilometros: Double, desnivelSubidaMetros: Int): Int? {
        if (!distanciaKilometros.isFinite() || distanciaKilometros <= 0.0) return null
        val horas = distanciaKilometros / KILOMETROS_POR_HORA_EN_LLANO +
            desnivelSubidaMetros.coerceAtLeast(0) / METROS_DE_SUBIDA_POR_HORA
        val minutos = horas * MINUTOS_POR_HORA
        if (minutos > MAXIMO_MINUTOS) return null
        return ((minutos / REDONDEO_MINUTOS).roundToInt() * REDONDEO_MINUTOS).coerceAtLeast(REDONDEO_MINUTOS)
    }
}
