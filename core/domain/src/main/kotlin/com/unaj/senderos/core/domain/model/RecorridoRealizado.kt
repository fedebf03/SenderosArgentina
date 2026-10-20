package com.unaj.senderos.core.domain.model

// El id sale del instante de inicio. Si se reemplaza un recorrido activo puede guardarse dos veces con el mismo id
// y solo cambia finMilisegundos (el guardado es idempotente)
data class RecorridoRealizado(
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
    val trazadoCaminado: List<Coordenada>,
    val esSimulado: Boolean,
    val estadoSincronizacion: EstadoSincronizacion
)
