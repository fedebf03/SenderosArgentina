package com.unaj.senderos.core.ui.revisionpuntos

import com.unaj.senderos.core.ui.comun.formatearUnDecimal
import kotlin.math.roundToInt

fun describirCantidadPuntosCapturados(cantidad: Int): String {
    return if (cantidad == 1) "1 punto capturado" else "$cantidad puntos capturados"
}

fun describirCantidadPuntosPorRevisar(cantidad: Int): String {
    return if (cantidad == 1) "1 punto por revisar" else "$cantidad puntos por revisar"
}

fun describirDistanciaAlTrazado(metros: Double): String {
    return if (metros < 1_000.0) {
        "a ${metros.roundToInt()} m del trazado"
    } else {
        "a ${formatearUnDecimal(metros / 1_000.0)} km del trazado"
    }
}

fun describirPuntoGuardadoConPosicionAntigua(antiguedadMilisegundos: Long): String {
    val segundos = (antiguedadMilisegundos / 1_000.0).roundToInt()
    return "Punto guardado con la posición de hace $segundos s (señal débil)"
}
