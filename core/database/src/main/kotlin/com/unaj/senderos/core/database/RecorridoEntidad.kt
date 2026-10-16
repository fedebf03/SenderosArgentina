package com.unaj.senderos.core.database

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
data class CoordenadaEntidad(
    val latitud: Double,
    val longitud: Double
)

@Entity(tableName = "recorridos", primaryKeys = ["idUsuario", "id"])
data class RecorridoEntidad(
    val id: String,
    val idUsuario: String,
    val idSendero: String,
    val nombreSendero: String,
    val inicioMilisegundos: Long,
    val finMilisegundos: Long,
    val duracionMilisegundos: Long,
    val distanciaKilometros: Double,
    val velocidadPromedioKilometrosPorHora: Double,
    val velocidadMaximaKilometrosPorHora: Double,
    val pasos: Int?,
    val ritmoPasosPorMinuto: Double?,
    val trazadoCaminado: List<CoordenadaEntidad>,
    val esSimulado: Boolean,
    val estadoSincronizacion: String,
    val fechaModificacion: Long
)
