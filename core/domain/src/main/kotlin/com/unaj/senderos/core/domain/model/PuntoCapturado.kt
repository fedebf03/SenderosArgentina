package com.unaj.senderos.domain.model

// Punto de interés marcado durante un recorrido; es local hasta que el usuario lo incorpora al sendero
data class PuntoCapturado(
    val id: String,
    val idUsuario: String,
    val idSendero: String,
    val latitud: Double,
    val longitud: Double,
    val tipo: TipoPunto,
    val nota: String,
    // Ruta local de la copia de la foto; vacío si el punto no tiene foto
    val foto: String,
    val instanteMilisegundos: Long
)
