package com.unaj.senderos.core.domain.model

data class LecturaUbicacion(
    val coordenada: Coordenada,
    val rumboGrados: Float?,
    val velocidadMetrosPorSegundo: Float?,
    val precisionMetros: Float?
)
