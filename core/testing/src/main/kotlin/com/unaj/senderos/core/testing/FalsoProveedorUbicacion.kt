package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.repository.ProveedorUbicacion

class FalsoProveedorUbicacion(
    var resultado: Result<Coordenada?> = Result.success(null)
) : ProveedorUbicacion {

    var cantidadConsultas: Int = 0
        private set

    // Contar la consulta y devolver el resultado configurado
    override suspend fun obtenerUbicacionActual(): Result<Coordenada?> {
        cantidadConsultas++
        return resultado
    }
}
