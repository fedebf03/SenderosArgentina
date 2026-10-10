package com.unaj.senderos.data.local

import kotlinx.serialization.Serializable

// Punto del recorrido tal como se guarda serializado dentro de la fila de Room
@Serializable
data class PuntoRecorridoEntidad(
    val latitud: Double,
    val longitud: Double,
    val tipo: String,
    val nombre: String
)
