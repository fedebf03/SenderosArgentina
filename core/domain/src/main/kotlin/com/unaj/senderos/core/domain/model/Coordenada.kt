package com.unaj.senderos.domain.model

data class Coordenada(
    val latitud: Double,
    val longitud: Double
)

fun Coordenada.tieneCoordenadasValidas(): Boolean {
    return latitud.isFinite() && longitud.isFinite() && latitud in -90.0..90.0 && longitud in -180.0..180.0
}
