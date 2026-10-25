package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.MuestraAcelerometro
import kotlinx.coroutines.flow.Flow

interface FuenteAcelerometro {

    // El sensor se libera cuando se deja de observar el flujo
    fun observarMuestras(): Flow<MuestraAcelerometro>
}
