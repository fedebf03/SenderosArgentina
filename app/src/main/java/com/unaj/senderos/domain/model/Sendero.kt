package com.unaj.senderos.domain.model

// Nivel de exigencia física de un sendero
enum class Dificultad { BAJA, MEDIA, ALTA }

// Forma en que se recorre un sendero
enum class TipoRecorrido { CIRCULAR, IDA_Y_VUELTA, LINEAL }

// Estado de sincronización de un sendero entre la base local y el servidor
enum class EstadoSincronizacion { PENDIENTE, SINCRONIZADO, CON_ERROR }

// Tramo de un sendero con su número de orden, nombre y distancia
data class Etapa(
    val numero: Int,
    val nombre: String,
    val distanciaKilometros: Double
)

// Modelo de datos inmutable que representa un sendero perteneciente a un usuario
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
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO
)
