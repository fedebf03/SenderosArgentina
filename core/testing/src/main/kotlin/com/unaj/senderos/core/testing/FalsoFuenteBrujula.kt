package com.unaj.senderos.falsos

import com.unaj.senderos.domain.repository.FuenteBrujula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FalsoFuenteBrujula : FuenteBrujula {

    // Rumbos que la prueba emite para simular la brújula
    val rumbos = MutableSharedFlow<Float>(extraBufferCapacity = 64)

    // Devolver el flujo que controla la prueba
    override fun observarRumbo(): Flow<Float> = rumbos
}
