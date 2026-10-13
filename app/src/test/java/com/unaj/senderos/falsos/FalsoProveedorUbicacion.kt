package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.port.ProveedorUbicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FalsoProveedorUbicacion(
    var resultado: Result<Coordenada?> = Result.success(null),
    var ubicaciones: Flow<LecturaUbicacion> = emptyFlow()
) : ProveedorUbicacion {

    var cantidadConsultas: Int = 0
        private set

    override suspend fun obtenerUbicacionActual(): Result<Coordenada?> {
        cantidadConsultas++
        return resultado
    }

    override fun observarUbicaciones(): Flow<LecturaUbicacion> = ubicaciones
}
