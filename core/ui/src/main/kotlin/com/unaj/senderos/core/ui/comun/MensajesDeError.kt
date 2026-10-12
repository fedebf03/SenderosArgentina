package com.unaj.senderos.presentation.comun

// Obtener el texto para el usuario: el mensaje de los errores de dominio (en español) o un respaldo propio para cualquier error técnico
fun describirErrorParaElUsuario(error: Throwable, respaldo: String): String {
    val esErrorDeDominio = error is IllegalStateException || error is IllegalArgumentException
    return if (esErrorDeDominio) error.message?.takeIf { texto -> texto.isNotBlank() } ?: respaldo else respaldo
}
