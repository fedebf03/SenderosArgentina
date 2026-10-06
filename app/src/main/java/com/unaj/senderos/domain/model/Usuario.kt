package com.unaj.senderos.domain.model

// Modelo de datos inmutable que representa a un usuario autenticado en el dominio
data class Usuario(
    val id: String,
    val email: String,
    val nombre: String = "",
    val fotoUrl: String = ""
)
