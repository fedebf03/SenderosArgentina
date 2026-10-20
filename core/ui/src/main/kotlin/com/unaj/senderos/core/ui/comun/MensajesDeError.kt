package com.unaj.senderos.core.ui.comun

// Los errores de dominio ya traen su mensaje en español. Para un error técnico se usa el respaldo
fun describirErrorParaElUsuario(error: Throwable, respaldo: String): String {
    val esErrorDeDominio = error is IllegalStateException || error is IllegalArgumentException
    return if (esErrorDeDominio) error.message?.takeIf { texto -> texto.isNotBlank() } ?: respaldo else respaldo
}
