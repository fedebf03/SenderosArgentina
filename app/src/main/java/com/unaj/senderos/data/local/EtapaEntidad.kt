package com.unaj.senderos.data.local

import kotlinx.serialization.Serializable

// Tramo de un sendero tal como se guarda serializado dentro de la fila de Room
@Serializable
data class EtapaEntidad(
    val numero: Int,
    val nombre: String,
    val distanciaKilometros: Double
)
