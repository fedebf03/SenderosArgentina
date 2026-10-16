package com.unaj.senderos.core.domain.model

fun filtrarSenderosPorTexto(senderos: List<Sendero>, texto: String): List<Sendero> {
    val textoNormalizado = texto.trim()
    if (textoNormalizado.isEmpty()) return senderos
    return senderos.filter { sendero ->
        sendero.nombre.contains(textoNormalizado, ignoreCase = true) ||
            sendero.region.contains(textoNormalizado, ignoreCase = true)
    }
}

fun filtrarSenderosPorDificultad(senderos: List<Sendero>, dificultades: Set<Dificultad>): List<Sendero> {
    if (dificultades.isEmpty()) return senderos
    return senderos.filter { sendero -> sendero.dificultad in dificultades }
}
