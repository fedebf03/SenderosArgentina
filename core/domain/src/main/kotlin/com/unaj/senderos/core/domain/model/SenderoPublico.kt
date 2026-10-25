package com.unaj.senderos.core.domain.model

data class SenderoPublico(
    val sendero: Sendero,
    val nombreAutor: String,
    val fechaPublicacion: Long = 0L
) {
    val id: String
        get() = sendero.id

    val idAutor: String
        get() = sendero.idUsuario
}
