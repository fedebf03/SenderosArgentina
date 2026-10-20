package com.unaj.senderos.core.domain.model

// Queda solo en el dispositivo hasta que el usuario lo incorpora al sendero
data class PuntoCapturado(
    val id: String,
    val idUsuario: String,
    val idSendero: String,
    val latitud: Double,
    val longitud: Double,
    val tipo: TipoPunto,
    val nota: String,
    val foto: String,
    val instanteMilisegundos: Long
)
