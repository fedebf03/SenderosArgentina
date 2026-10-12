package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.MuestraAcelerometro
import com.unaj.senderos.domain.repository.FuenteAcelerometro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FalsoFuenteAcelerometro : FuenteAcelerometro {

    // Muestras que la prueba emite para simular el acelerómetro
    val muestras = MutableSharedFlow<MuestraAcelerometro>(extraBufferCapacity = 4096)

    // Devolver el flujo que controla la prueba
    override fun observarMuestras(): Flow<MuestraAcelerometro> = muestras
}
