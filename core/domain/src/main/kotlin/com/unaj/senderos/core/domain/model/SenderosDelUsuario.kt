package com.unaj.senderos.core.domain.model

data class SenderosDelUsuario(
    val usuario: Usuario,
    val senderos: List<Sendero>
)
