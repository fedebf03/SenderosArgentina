package com.unaj.senderos.falsos

import com.unaj.senderos.domain.port.FuenteBrujula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FalsoFuenteBrujula : FuenteBrujula {

    val rumbos = MutableSharedFlow<Float>(extraBufferCapacity = 64)

    override fun observarRumbo(): Flow<Float> = rumbos
}
