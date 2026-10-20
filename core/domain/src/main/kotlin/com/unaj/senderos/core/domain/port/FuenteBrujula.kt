package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.Flow

interface FuenteBrujula {

    // Rumbo en grados [0, 360). Solo se emiten los cambios apreciables
    fun observarRumbo(): Flow<Float>
}
