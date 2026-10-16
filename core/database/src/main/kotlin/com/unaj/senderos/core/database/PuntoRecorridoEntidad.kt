package com.unaj.senderos.core.database

import kotlinx.serialization.Serializable

@Serializable
data class PuntoRecorridoEntidad(
    val latitud: Double,
    val longitud: Double,
    val tipo: String,
    val nombre: String,
    val foto: String = ""
)
