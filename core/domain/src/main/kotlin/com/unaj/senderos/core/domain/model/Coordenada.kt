package com.unaj.senderos.domain.model

// Posición geográfica del usuario o de cualquier punto, en grados decimales
data class Coordenada(
    val latitud: Double,
    val longitud: Double
)

// Verificar que la latitud y la longitud son finitas y están dentro de los rangos geográficos
fun Coordenada.tieneCoordenadasValidas(): Boolean {
    return latitud.isFinite() && longitud.isFinite() && latitud in -90.0..90.0 && longitud in -180.0..180.0
}
