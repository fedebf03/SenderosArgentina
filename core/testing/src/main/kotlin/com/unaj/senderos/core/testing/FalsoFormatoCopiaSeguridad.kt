package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.port.FormatoCopiaSeguridad

// Formato de prueba: recuerda los senderos por el texto que devolvió, sin serializar de verdad
class FalsoFormatoCopiaSeguridad : FormatoCopiaSeguridad {

    private val guardados = mutableMapOf<String, List<Sendero>>()

    // Devolver un texto que identifica la lista recibida
    override fun escribir(senderos: List<Sendero>, instanteMilisegundos: Long): String {
        val texto = "copia-${guardados.size + 1}"
        guardados[texto] = senderos
        return texto
    }

    // Devolver la lista asociada al texto o un fallo si no la conoce
    override fun leer(texto: String): Result<List<Sendero>> =
        guardados[texto]?.let { senderos -> Result.success(senderos) }
            ?: Result.failure(IllegalArgumentException("El archivo no es una copia de seguridad de Senderos"))

    // Registrar una copia para que leer la encuentre
    fun registrar(texto: String, senderos: List<Sendero>) {
        guardados[texto] = senderos
    }
}
