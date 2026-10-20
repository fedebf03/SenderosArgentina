package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.Flow

fun interface FuentePresion {

    // El flujo termina con error si el equipo no tiene barómetro
    fun observarPresion(): Flow<Double>
}
