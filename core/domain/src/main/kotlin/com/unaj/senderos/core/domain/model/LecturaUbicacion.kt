package com.unaj.senderos.domain.model

// Lectura del GPS: posición más rumbo y velocidad cuando el sistema los informa
data class LecturaUbicacion(
    val coordenada: Coordenada,
    val rumboGrados: Float?,
    val velocidadMetrosPorSegundo: Float?,
    val precisionMetros: Float?
)
