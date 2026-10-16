package com.unaj.senderos.core.network

data class SenderoPublicoRed(
    val idAutor: String = "",
    val nombreAutor: String = "",
    val fechaPublicacion: Long = 0L,
    val sendero: SenderoRed = SenderoRed()
)
