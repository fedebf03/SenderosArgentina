package com.unaj.senderos.domain.model

// Situación de un recorrido en curso; el trazado incluye el tramo de cierre (circular) o la vuelta (ida y vuelta)
data class EstadoSeguimiento(
    val idSendero: String,
    val trazado: List<PuntoRecorrido>,
    val indicesRecorridoDelTrazado: List<Int> = trazado.indices.toList(),
    val posicionActual: Coordenada? = null,
    val ultimaPosicionAceptada: Coordenada? = null,
    val distanciaRecorridaKilometros: Double = 0.0,
    val distanciaRestanteKilometros: Double = 0.0,
    val distanciaAlTrazadoMetros: Double = 0.0,
    val indiceTramoActual: Int = 0,
    val indiceProximoPunto: Int = 0,
    val rumboGrados: Float? = null,
    val velocidadMetrosPorSegundo: Float? = null,
    val esSimulado: Boolean = false,
    val multiplicadorSimulacion: Int = 1,
    val estaDesviado: Boolean = false,
    val indicesPuntosAvisados: Set<Int> = emptySet(),
    val instanteInicioMilisegundos: Long = 0L,
    val tiempoTranscurridoMilisegundos: Long = 0L
)
