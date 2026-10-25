package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.clima.PronosticoClima
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.AlmacenPronosticos
import com.unaj.senderos.core.domain.port.FuentePronostico
import com.unaj.senderos.core.domain.port.Reloj
import javax.inject.Inject
import kotlin.math.roundToInt

private const val VIGENCIA_MILISEGUNDOS = 30 * 60_000L
private const val CENTESIMAS_POR_GRADO = 100

data class ResultadoPronostico(
    val pronostico: PronosticoClima,
    val antiguedadMilisegundos: Long,
    val estaDesactualizado: Boolean,
    val mensajeError: String?
)

class ObtenerPronosticoUseCase @Inject constructor(
    private val fuentePronostico: FuentePronostico,
    private val almacenPronosticos: AlmacenPronosticos,
    private val reloj: Reloj
) {
    // Se usa el guardado si tiene menos de 30 minutos. Sin red se cae al último guardado, marcado como viejo, para que
    // el usuario vea algo útil en el campo
    suspend operator fun invoke(lugar: Coordenada, forzar: Boolean = false): Result<ResultadoPronostico> {
        val clave = claveDe(lugar)
        val guardado = almacenPronosticos.leer(clave)
        val ahora = reloj()
        if (!forzar && guardado != null && ahora - guardado.obtenidoMilisegundos < VIGENCIA_MILISEGUNDOS) {
            return Result.success(ResultadoPronostico(guardado, ahora - guardado.obtenidoMilisegundos, estaDesactualizado = false, mensajeError = null))
        }
        return fuentePronostico.obtener(lugar).fold(
            onSuccess = { nuevo ->
                almacenPronosticos.guardar(clave, nuevo)
                Result.success(ResultadoPronostico(nuevo, (ahora - nuevo.obtenidoMilisegundos).coerceAtLeast(0L), estaDesactualizado = false, mensajeError = null))
            },
            onFailure = { error ->
                if (guardado == null) {
                    Result.failure(error)
                } else {
                    Result.success(ResultadoPronostico(guardado, ahora - guardado.obtenidoMilisegundos, estaDesactualizado = true, mensajeError = error.message))
                }
            }
        )
    }

    companion object {
        // Se redondea a la centésima de grado (cerca de un kilómetro) y la clave sirve como nombre de archivo
        fun claveDe(lugar: Coordenada): String {
            val latitud = (lugar.latitud * CENTESIMAS_POR_GRADO).roundToInt()
            val longitud = (lugar.longitud * CENTESIMAS_POR_GRADO).roundToInt()
            return "clima_${latitud}_$longitud"
        }
    }
}
