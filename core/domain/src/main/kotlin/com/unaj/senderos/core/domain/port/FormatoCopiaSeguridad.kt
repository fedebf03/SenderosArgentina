package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Sendero

interface FormatoCopiaSeguridad {
    fun escribir(senderos: List<Sendero>, instanteMilisegundos: Long): String

    fun leer(texto: String): Result<List<Sendero>>
}
