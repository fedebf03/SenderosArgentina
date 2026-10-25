package com.unaj.senderos.core.domain.model

data class Usuario(
    val id: String,
    val email: String,
    val nombre: String = "",
    val fotoUrl: String = "",
    val pais: String = "",
    val provincia: String = "",
    val localidad: String = ""
)
