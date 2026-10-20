package com.unaj.senderos.core.network.recorrido

// Los valores por defecto los necesita el deserializador de Firestore
data class CoordenadaRed(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)

// El idUsuario no se guarda porque ya va en la ruta /usuarios/{idUsuario}/recorridos/{id}
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
