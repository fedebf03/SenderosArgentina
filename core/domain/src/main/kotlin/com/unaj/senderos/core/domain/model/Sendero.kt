package com.unaj.senderos.domain.model

enum class Dificultad { BAJA, MEDIA, ALTA }

enum class TipoRecorrido { CIRCULAR, IDA_Y_VUELTA, LINEAL }

enum class EstadoSincronizacion { PENDIENTE, SINCRONIZADO, CON_ERROR }

data class Etapa(
    val numero: Int,
    val nombre: String,
    val distanciaKilometros: Double
)

data class Sendero(
    val id: String,
    val idUsuario: String,
    val nombre: String,
    val region: String,
    val distanciaKilometros: Double,
    val dificultad: Dificultad,
    val descripcion: String = "",
    val desnivelMetros: Int = 0,
    val duracionEstimada: String = "",
    val alturaMaximaMetros: Int = 0,
    val alturaMinimaMetros: Int = 0,
    val tipoRecorrido: TipoRecorrido = TipoRecorrido.LINEAL,
    val puntoInicio: String = "",
    val mejorEpoca: String = "",
    val recomendaciones: List<String> = emptyList(),
    val servicios: List<String> = emptyList(),
    val etapas: List<Etapa> = emptyList(),
    val esFavorito: Boolean = false,
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
    val recorrido: List<PuntoRecorrido> = emptyList(),
    val foto: String = ""
)
