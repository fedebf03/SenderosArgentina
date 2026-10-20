package com.unaj.senderos.core.domain.model

// Valores en m/s2. Todas las muestras de un recorrido usan el mismo origen de tiempo
data class MuestraAcelerometro(
    val x: Float,
    val y: Float,
    val z: Float,
    val instanteMilisegundos: Long
)
