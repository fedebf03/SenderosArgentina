package com.unaj.senderos.data.remote

// Posición del trazado caminado como se guarda en Firestore (valores por defecto requeridos por el deserializador)
data class CoordenadaRed(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)

// Documento de recorrido en /usuarios/{idUsuario}/recorridos/{id}; el idUsuario va en la ruta
data class RecorridoRed(
    val id: String = "",
    val idSendero: String = "",
    val nombreSendero: String = "",
    val inicioMilisegundos: Long = 0L,
    val finMilisegundos: Long = 0L,
    val duracionMilisegundos: Long = 0L,
    val distanciaKilometros: Double = 0.0,
    val velocidadPromedioKilometrosPorHora: Double = 0.0,
    val velocidadMaximaKilometrosPorHora: Double = 0.0,
    val pasos: Int? = null,
    val ritmoPasosPorMinuto: Double? = null,
    val trazadoCaminado: List<CoordenadaRed> = emptyList(),
    val esSimulado: Boolean = false,
    val fechaModificacion: Long = 0L
)
