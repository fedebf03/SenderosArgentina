package com.unaj.senderos.core.network.senderopublico

import com.unaj.senderos.core.network.sendero.SenderoRed

data class SenderoPublicoRed(
    val idAutor: String = "",
    val nombreAutor: String = "",
    val fechaPublicacion: Long = 0L,
    val sendero: SenderoRed = SenderoRed()
)
