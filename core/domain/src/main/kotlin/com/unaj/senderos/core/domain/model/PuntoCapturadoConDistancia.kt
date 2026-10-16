package com.unaj.senderos.core.domain.model

// Distancia al trazado a partir de la cual la UI debería advertir al usuario y dejar el punto desmarcado
const val DISTANCIA_MAXIMA_RECOMENDADA_METROS = 100.0

// Punto capturado junto con su distancia en metros al trazado del sendero; null si el sendero no tiene recorrido con qué comparar
data class PuntoCapturadoConDistancia(
    val punto: PuntoCapturado,
    val distanciaAlTrazadoMetros: Double?
) {
    val superaDistanciaRecomendada: Boolean
        get() = distanciaAlTrazadoMetros != null && distanciaAlTrazadoMetros > DISTANCIA_MAXIMA_RECOMENDADA_METROS
}
