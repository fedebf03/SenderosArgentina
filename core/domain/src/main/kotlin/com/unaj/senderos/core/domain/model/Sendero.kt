package com.unaj.senderos.domain.model

// Nivel de exigencia física de un sendero
enum class Dificultad { BAJA, MEDIA, ALTA }

// Modelo de datos inmutable que representa un sendero perteneciente a un usuario
data class Sendero(
    val id: String,
    val idUsuario: String,
    val nombre: String,
    val region: String,
    val distanciaKilometros: Double,
    val dificultad: Dificultad
)
