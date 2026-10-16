package com.unaj.senderos.core.database

import androidx.room.Entity

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
