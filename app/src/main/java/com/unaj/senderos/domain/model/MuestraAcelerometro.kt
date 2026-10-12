package com.unaj.senderos.domain.model

// Lectura del acelerómetro en m/s2 con su instante en milisegundos (mismo origen de tiempo para todas las muestras de un recorrido)
data class MuestraAcelerometro(
    val x: Float,
    val y: Float,
    val z: Float,
    val instanteMilisegundos: Long
)
