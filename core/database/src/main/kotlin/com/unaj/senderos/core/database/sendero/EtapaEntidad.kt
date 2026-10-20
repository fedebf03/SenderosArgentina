package com.unaj.senderos.core.database.sendero

import kotlinx.serialization.Serializable

@Serializable
data class EtapaEntidad(
    val numero: Int,
    val nombre: String,
    val distanciaKilometros: Double
)
