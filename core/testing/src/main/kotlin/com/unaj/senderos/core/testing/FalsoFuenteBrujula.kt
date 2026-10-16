package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.FuenteBrujula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FalsoFuenteBrujula : FuenteBrujula {

    val rumbos = MutableSharedFlow<Float>(extraBufferCapacity = 64)

    override fun observarRumbo(): Flow<Float> = rumbos
}
