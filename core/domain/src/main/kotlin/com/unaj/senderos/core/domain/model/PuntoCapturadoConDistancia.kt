package com.unaj.senderos.core.domain.model

// A partir de esta distancia al trazado se advierte al usuario y el punto queda desmarcado
const val DISTANCIA_MAXIMA_RECOMENDADA_METROS = 100.0

// La distancia queda nula si el sendero no tiene recorrido con qué comparar
data class PuntoCapturadoConDistancia(
    val punto: PuntoCapturado,
    val distanciaAlTrazadoMetros: Double?
) {
    val superaDistanciaRecomendada: Boolean
        get() = distanciaAlTrazadoMetros != null && distanciaAlTrazadoMetros > DISTANCIA_MAXIMA_RECOMENDADA_METROS
}
