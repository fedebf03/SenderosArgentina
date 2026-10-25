package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.MuestraAcelerometro
import com.unaj.senderos.core.domain.port.FuenteAcelerometro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FalsoFuenteAcelerometro : FuenteAcelerometro {

    val muestras = MutableSharedFlow<MuestraAcelerometro>(extraBufferCapacity = 4096)

    override fun observarMuestras(): Flow<MuestraAcelerometro> = muestras
}
