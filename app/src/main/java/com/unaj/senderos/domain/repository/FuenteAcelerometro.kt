package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.MuestraAcelerometro
import kotlinx.coroutines.flow.Flow

interface FuenteAcelerometro {

    // Observar las muestras del acelerómetro mientras haya un recolector; el sensor se libera al cancelar la recolección
    fun observarMuestras(): Flow<MuestraAcelerometro>
}
