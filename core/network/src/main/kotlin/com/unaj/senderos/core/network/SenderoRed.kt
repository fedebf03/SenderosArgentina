package com.unaj.senderos.core.network

// Tramo de un sendero como se guarda en Firestore (valores por defecto requeridos por el deserializador)
data class EtapaRed(
    val numero: Int = 0,
    val nombre: String = "",
    val distanciaKilometros: Double = 0.0
)

// Documento de sendero en /usuarios/{idUsuario}/senderos/{id}; el idUsuario va en la ruta
data class SenderoRed(
    val id: String = "",
    val nombre: String = "",
    val region: String = "",
    val distanciaKilometros: Double = 0.0,
    val dificultad: String = "",
    val descripcion: String = "",
    val desnivelMetros: Int = 0,
    val duracionEstimada: String = "",
    val alturaMaximaMetros: Int = 0,
    val alturaMinimaMetros: Int = 0,
    val tipoRecorrido: String = "",
    val puntoInicio: String = "",
    val mejorEpoca: String = "",
    val recomendaciones: List<String> = emptyList(),
    val servicios: List<String> = emptyList(),
    val etapas: List<EtapaRed> = emptyList(),
    val esFavorito: Boolean = false,
    val fechaModificacion: Long = 0L,
    val eliminado: Boolean = false,
    val recorrido: List<PuntoRecorridoRed> = emptyList(),
    val foto: String = "",
    val bioma: String = "SIN_DEFINIR",
    val idOrigen: String = "",
    // Un documento anterior al perfil no trae el campo y se lee como null (sin calcular)
    val perfilElevacion: PerfilElevacionRed? = null
)

// Punto del recorrido como se guarda en Firestore (valores por defecto requeridos por el deserializador)
data class PuntoRecorridoRed(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val tipo: String = "PASO",
    val nombre: String = "",
    val foto: String = ""
)
