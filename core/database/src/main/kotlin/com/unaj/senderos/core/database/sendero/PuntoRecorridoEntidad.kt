package com.unaj.senderos.data.local

import kotlinx.serialization.Serializable

@Serializable
data class PuntoRecorridoEntidad(
    val latitud: Double,
    val longitud: Double,
    val tipo: String,
    val nombre: String,
    val foto: String = ""
)
