package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.Flow

interface FuenteBrujula {

    // Rumbo en grados [0, 360). Solo se informan los cambios que se notan
    fun observarRumbo(): Flow<Float>
}
