package com.unaj.senderos.data.local

import kotlinx.serialization.Serializable

@Serializable
data class EtapaEntidad(
    val numero: Int,
    val nombre: String,
    val distanciaKilometros: Double
)
