package com.unaj.senderos.core.domain.model

// El trazado incluye el tramo de cierre (circular) o la vuelta (ida y vuelta)
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
    // Se agrega la primera lectura y luego cada lectura válida a 5 m o más del último punto. Las imprecisas (hasta 50 m)
    // se agregan si están separadas al menos su precisión, pero no suman distancia ni disparan avisos
    val trazadoCaminado: List<Coordenada> = emptyList(),
    // Última lectura con precisión suficiente que no llegó a agregarse al trazado caminado. Se agrega al cerrar el recorrido
    val ultimaPosicionConocida: Coordenada? = null,
    // Es la velocidad que informa el GPS, sin corregir (en simulación ya viene multiplicada)
    val velocidadMaximaMetrosPorSegundo: Float = 0f,
    val esSimulado: Boolean = false,
    val multiplicadorSimulacion: Int = 1,
    val estaDesviado: Boolean = false,
    val llegoAlFinal: Boolean = false,
    val indicesPuntosAvisados: Set<Int> = emptySet(),
    val instanteInicioMilisegundos: Long = 0L,
    val tiempoTranscurridoMilisegundos: Long = 0L,
    // Sirve para calcular cuánto pudo moverse el usuario, como máximo, entre dos lecturas
    val instanteUltimaLecturaMilisegundos: Long? = null,
    val esCircular: Boolean = false,
    // Nulo si el equipo no tiene acelerómetro. En simulación se estima un paso cada 0,75 m
    val pasos: Int? = null,
    // Pasos por minuto en los últimos 10 s
    val ritmoPasosPorMinuto: Double? = null,
    // Se considera detenido con menos de 0,5 m/s (o sin velocidad informada) y sin pasos en los últimos 10 s
    val estaDetenido: Boolean = false,
    // Rumbo magnético del vector de rotación. Queda nulo sin brújula o en simulación
    val rumboBrujulaGrados: Float? = null,
    // BUENA si la última lectura válida tiene 15 s o menos y DEBIL si no. En simulación siempre es BUENA
    val calidadSenal: CalidadSenal = CalidadSenal.BUSCANDO,
    // Una lectura es válida con precisión de 30 m o mejor (o sin precisión informada)
    val instanteUltimaLecturaValidaMilisegundos: Long? = null,
    // En una caminata libre no hay trazado que seguir. Por lo tanto no hay restante, desvío ni llegada
    val esCaminataLibre: Boolean = false
) {

    // Sin movimiento el GPS no da un rumbo fiable, por eso se usa la brújula si la hay. En movimiento se usa el del GPS
    val rumboEfectivoGrados: Float?
        get() = if (estaDetenido && rumboBrujulaGrados != null) rumboBrujulaGrados else rumboGrados

    val velocidadPromedioKilometrosPorHora: Double
        get() = if (tiempoTranscurridoMilisegundos <= 0L) {
            0.0
        } else {
            distanciaRecorridaKilometros / (tiempoTranscurridoMilisegundos / MILISEGUNDOS_POR_HORA)
        }
}

private const val MILISEGUNDOS_POR_HORA = 3_600_000.0
