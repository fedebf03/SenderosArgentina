package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Sendero

interface FormatoCopiaSeguridad {
    // Convertir los senderos en el texto de una copia de seguridad, con el instante en que se exportó
    fun escribir(senderos: List<Sendero>, instanteMilisegundos: Long): String

    // Leer los senderos de una copia de seguridad; falla con un mensaje en español si el texto no es una copia de esta aplicación
    fun leer(texto: String): Result<List<Sendero>>
}
