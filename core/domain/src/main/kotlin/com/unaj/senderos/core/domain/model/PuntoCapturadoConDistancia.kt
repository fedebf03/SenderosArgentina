package com.unaj.senderos.domain.model

import com.unaj.senderos.domain.DISTANCIA_MAXIMA_RECOMENDADA_METROS

// Punto capturado junto con su distancia en metros al trazado del sendero; null si el sendero no tiene recorrido con qué comparar
data class PuntoCapturadoConDistancia(
    val punto: PuntoCapturado,
    val distanciaAlTrazadoMetros: Double?
) {
    // Indica si el punto está a más de 100 m del trazado y la UI debería advertirlo y dejarlo desmarcado por defecto
    val superaDistanciaRecomendada: Boolean
        get() = distanciaAlTrazadoMetros != null && distanciaAlTrazadoMetros > DISTANCIA_MAXIMA_RECOMENDADA_METROS
}
