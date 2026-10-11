package com.unaj.senderos.domain.model

data class SenderosDelUsuario(
    val usuario: Usuario,
    val senderos: List<Sendero>
)
