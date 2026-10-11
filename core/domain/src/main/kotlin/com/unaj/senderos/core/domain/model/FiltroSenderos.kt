package com.unaj.senderos.domain.model

fun filtrarSenderosPorTexto(senderos: List<Sendero>, texto: String): List<Sendero> {
    val textoNormalizado = texto.trim()
    if (textoNormalizado.isEmpty()) return senderos
    return senderos.filter { sendero ->
        sendero.nombre.contains(textoNormalizado, ignoreCase = true) ||
            sendero.region.contains(textoNormalizado, ignoreCase = true)
    }
}
