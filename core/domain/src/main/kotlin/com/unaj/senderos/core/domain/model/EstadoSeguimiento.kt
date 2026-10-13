package com.unaj.senderos.domain.model

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
    // Trazado dibujado y guardado en el historial: la primera lectura recibida y cada lectura con precisión de hasta el doble del umbral vigente
    // a 5 m o más de la anterior del trazado (o válida tras un hueco de más de 30 s), en orden de llegada. Las imprecisas entran aquí pero no mueven distancia ni avisos.
    // Limitación: no tiene tope; cada lectura aceptada copia la lista (O(n)) y el recorrido guardado viaja en un único documento de Firestore de hasta 1 MiB
    val trazadoCaminado: List<Coordenada> = emptyList(),
    // Instante del último punto agregado al trazado caminado, para detectar huecos de más de 30 s; null si todavía no hay ninguno
    val instanteUltimoPuntoTrazadoMilisegundos: Long? = null,
    // Última posición recibida con precisión suficiente para el trazado (aunque no fuera válida para el seguimiento); se agrega al cerrar el recorrido
    val ultimaPosicionConocida: Coordenada? = null,
    // Mayor velocidad reportada por las lecturas tal cual llegan (en simulación con multiplicador mayor a 1 refleja la velocidad simulada); 0 si ninguna la informó
    val velocidadMaximaMetrosPorSegundo: Float = 0f,
    val esSimulado: Boolean = false,
    val multiplicadorSimulacion: Int = 1,
    val estaDesviado: Boolean = false,
    val llegoAlFinal: Boolean = false,
    val indicesPuntosAvisados: Set<Int> = emptySet(),
    val instanteInicioMilisegundos: Long = 0L,
    val tiempoTranscurridoMilisegundos: Long = 0L,
    // Instante de la lectura anterior, para acotar el desplazamiento plausible entre lecturas; nulo antes de la primera
    val instanteUltimaLecturaMilisegundos: Long? = null,
    val esCircular: Boolean = false,
    // Pasos contados desde el inicio; null si el equipo no tiene acelerómetro (en simulación se estiman a 1 cada 0,75 m)
    val pasos: Int? = null,
    // Pasos por minuto en los últimos 10 s; null si el equipo no mide pasos
    val ritmoPasosPorMinuto: Double? = null,
    // Verdadero si la velocidad es menor a 0,5 m/s (o desconocida) y no hubo pasos en 10 s
    val estaDetenido: Boolean = false,
    // Rumbo magnético de la brújula (vector de rotación); null si el equipo no la tiene o en simulación
    val rumboBrujulaGrados: Float? = null,
    // BUSCANDO hasta la primera lectura válida; BUENA si la última válida tiene 15 s o menos; DEBIL si no (siempre BUENA en simulación)
    val calidadSenal: CalidadSenal = CalidadSenal.BUSCANDO,
    // Instante de la última lectura válida (precisión de 30 m o mejor, o sin precisión informada); null si todavía no hubo ninguna
    val instanteUltimaLecturaValidaMilisegundos: Long? = null
) {

    // Rumbo a mostrar: el de la brújula si el usuario está detenido y hay brújula (el GPS no da rumbo fiable sin movimiento);
    // en movimiento, o sin brújula, el último del GPS
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
