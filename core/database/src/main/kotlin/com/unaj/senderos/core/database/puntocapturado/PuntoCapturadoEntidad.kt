package com.unaj.senderos.data.local

import androidx.room.Entity

// Fila de Room de un punto de interés capturado durante un recorrido; local hasta incorporarse al sendero
@Entity(tableName = "puntos_capturados", primaryKeys = ["idUsuario", "id"])
data class PuntoCapturadoEntidad(
    val id: String,
    val idUsuario: String,
    val idSendero: String,
    val latitud: Double,
    val longitud: Double,
    val tipo: String,
    val nota: String,
    val foto: String,
    val instanteMilisegundos: Long
)
