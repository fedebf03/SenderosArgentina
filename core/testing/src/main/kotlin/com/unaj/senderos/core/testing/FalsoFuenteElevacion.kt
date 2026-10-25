package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.FuenteElevacion

class FalsoFuenteElevacion : FuenteElevacion {

    // Altitud que crece 100 m por cada 0,01 grado que la latitud baja desde -40
    var altitudDe: (Coordenada) -> Double = { coordenada -> 1000.0 + (-40.0 - coordenada.latitud) * 10_000.0 }

    var falloDevuelto: Throwable? = null

    // Excepción lanzada tal cual, para simular la cancelación
    var excepcionLanzada: Throwable? = null

    val llamadas: MutableList<List<Coordenada>> = mutableListOf()

    override suspend fun obtenerAltitudes(coordenadas: List<Coordenada>): Result<List<Double>> {
        llamadas.add(coordenadas)
        excepcionLanzada?.let { excepcion -> throw excepcion }
        falloDevuelto?.let { fallo -> return Result.failure(fallo) }
        return Result.success(coordenadas.map(altitudDe))
    }
}
