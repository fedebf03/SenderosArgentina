package com.unaj.senderos.domain.model

// Recorrido terminado por el usuario sobre un sendero; los pasos y el ritmo son nulos si el equipo no los mide.
// Limitaciones: el trazado caminado no tiene tope y todo el recorrido se sube como un único documento de Firestore (máximo 1 MiB).
// El id sale del instante de inicio: si se reemplaza un recorrido activo puede guardarse dos veces con el mismo id (guardado idempotente) y solo cambia finMilisegundos
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
