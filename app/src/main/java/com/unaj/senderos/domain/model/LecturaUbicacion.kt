package com.unaj.senderos.domain.model

data class LecturaUbicacion(
    val coordenada: Coordenada,
    val rumboGrados: Float?,
    val velocidadMetrosPorSegundo: Float?,
    val precisionMetros: Float?
)
