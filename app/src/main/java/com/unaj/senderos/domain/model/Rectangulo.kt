package com.unaj.senderos.domain.model

// Rectángulo geográfico en grados decimales
data class Rectangulo(
    val sur: Double,
    val oeste: Double,
    val norte: Double,
    val este: Double
)
