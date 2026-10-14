package com.unaj.senderos.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "senderos", primaryKeys = ["idUsuario", "id"])
data class SenderoEntidad(
    val id: String,
    val idUsuario: String,
    val nombre: String,
    val region: String,
    val distanciaKilometros: Double,
    val dificultad: String,
    val descripcion: String,
    val desnivelMetros: Int,
    val duracionEstimada: String,
    val alturaMaximaMetros: Int,
    val alturaMinimaMetros: Int,
    val tipoRecorrido: String,
    val puntoInicio: String,
    val mejorEpoca: String,
    val recomendaciones: List<String>,
    val servicios: List<String>,
    val etapas: List<EtapaEntidad>,
    val esFavorito: Boolean,
    val fechaModificacion: Long,
    val estadoSincronizacion: String,
    val estaEliminado: Boolean,
    @ColumnInfo(defaultValue = "[]")
    val recorrido: List<PuntoRecorridoEntidad> = emptyList(),
    @ColumnInfo(defaultValue = "")
    val foto: String = "",
    @ColumnInfo(defaultValue = "SIN_DEFINIR")
    val bioma: String = "SIN_DEFINIR",
    @ColumnInfo(defaultValue = "")
    val idOrigen: String = "",
    // JSON del perfil de elevación; null si todavía no se calculó
    val perfilElevacion: PerfilElevacionEntidad? = null
)
