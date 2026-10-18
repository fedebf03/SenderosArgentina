package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.port.FuenteElevacion
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import javax.inject.Inject

// Calcula el perfil de elevación de un trazado sin guardarlo, por ejemplo mientras se crea o edita el sendero
class CalcularPerfilDeTrazadoUseCase @Inject constructor(private val fuenteElevacion: FuenteElevacion) {

    // Muestrear el trazado, pedir las alturas a la fuente y armar el perfil con la firma del trazado; falla si hay menos de dos puntos
    // válidos o si la fuente no responde (sin conexión, límite del servicio)
    suspend operator fun invoke(recorrido: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): Result<PerfilElevacion> {
        val muestras = CalculadoraPerfil.muestrearTrazado(recorrido, tipoRecorrido)
        if (muestras.isEmpty()) {
            return Result.failure(
                IllegalStateException("El sendero necesita al menos dos puntos de recorrido para calcular el perfil")
            )
        }
        val altitudes = fuenteElevacion.obtenerAltitudes(muestras.map { muestra -> muestra.coordenada })
            .getOrElse { fallo -> return Result.failure(fallo) }
        return try {
            Result.success(CalculadoraPerfil.calcular(muestras, altitudes, CalculadoraPerfil.firmaTrazado(recorrido, tipoRecorrido)))
        } catch (error: IllegalArgumentException) {
            Result.failure(error)
        }
    }
}
