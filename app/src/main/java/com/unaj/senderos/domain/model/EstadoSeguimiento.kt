package com.unaj.senderos.domain.model

// Situación de un recorrido en curso; el trazado incluye el tramo de cierre (circular) o la vuelta (ida y vuelta)
data class EstadoSeguimiento(
    val idSendero: String,
    val trazado: List<PuntoRecorrido>,
    val nombreSendero: String = "",
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
    // Posiciones aceptadas (la primera y cada una a 5 m o más de la anterior aceptada), en orden de llegada.
    // Limitación: no tiene tope; cada lectura aceptada copia la lista (O(n)) y el recorrido guardado viaja en un único documento de Firestore de hasta 1 MiB
    val trazadoCaminado: List<Coordenada> = emptyList(),
    // Mayor velocidad reportada por las lecturas tal cual llegan (en simulación con multiplicador mayor a 1 refleja la velocidad simulada); 0 si ninguna la informó
    val velocidadMaximaMetrosPorSegundo: Float = 0f,
    val esSimulado: Boolean = false,
    val multiplicadorSimulacion: Int = 1,
    val estaDesviado: Boolean = false,
    val llegoAlFinal: Boolean = false,
    val indicesPuntosAvisados: Set<Int> = emptySet(),
    val instanteInicioMilisegundos: Long = 0L,
    val tiempoTranscurridoMilisegundos: Long = 0L
)
