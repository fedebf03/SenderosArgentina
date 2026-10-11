package com.unaj.senderos.domain.model

data class Usuario(
    val id: String,
    val email: String,
    val nombre: String = "",
    val fotoUrl: String = ""
)
