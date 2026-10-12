package com.unaj.senderos.domain.repository

import kotlinx.coroutines.flow.Flow

interface FuenteBrujula {

    // Observar el rumbo magnético en grados [0, 360) mientras haya un recolector, emitiendo solo cambios apreciables
    fun observarRumbo(): Flow<Float>
}
