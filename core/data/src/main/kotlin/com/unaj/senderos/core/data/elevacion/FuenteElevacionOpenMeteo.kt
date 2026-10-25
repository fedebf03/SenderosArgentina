package com.unaj.senderos.core.data.elevacion

import android.util.Log
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.ErrorElevacionException
import com.unaj.senderos.core.domain.port.FuenteElevacion
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import com.unaj.senderos.core.network.openmeteo.ApiElevacionOpenMeteo
import javax.inject.Inject

private const val TAG = "ELEVACION"
private const val MAXIMO_COORDENADAS_POR_PEDIDO = 100
private const val CODIGO_DEMASIADOS_PEDIDOS = 429
private const val CODIGO_ERROR_CLIENTE_MINIMO = 400
private const val CODIGO_ERROR_SERVIDOR_MINIMO = 500

class FuenteElevacionOpenMeteo @Inject constructor(
    private val api: ApiElevacionOpenMeteo
) : FuenteElevacion {

    // La API acepta hasta 100 coordenadas por pedido, por eso se mandan en grupos de 100 sin cambiar el orden
    override suspend fun obtenerAltitudes(coordenadas: List<Coordenada>): Result<List<Double>> {
        return try {
            val altitudes = coordenadas.chunked(MAXIMO_COORDENADAS_POR_PEDIDO).flatMap { lote -> pedirLote(lote) }
            Result.success(altitudes)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: ErrorElevacionException) {
            Log.e(TAG, "No se pudieron obtener las alturas: ${error.message}", error)
            Result.failure(error)
        } catch (error: Exception) {
            Log.e(TAG, "Fallo inesperado al obtener las alturas", error)
            Result.failure(ErrorElevacionException("No se pudo calcular el perfil de elevación", error))
        }
    }

    private suspend fun pedirLote(lote: List<Coordenada>): List<Double> {
        val respuesta = try {
            Log.d(TAG, "Pidiendo ${lote.size} alturas a Open-Meteo")
            api.obtenerElevaciones(
                latitudes = lote.joinToString(",") { coordenada -> formatearGrados(coordenada.latitud) },
                longitudes = lote.joinToString(",") { coordenada -> formatearGrados(coordenada.longitud) }
            )
        } catch (error: IOException) {
            throw SinConexionElevacionException(error)
        } catch (error: HttpException) {
            val codigo = error.code()
            throw ErrorElevacionException(
                mensaje = describirCodigoHttp(codigo),
                causa = error,
                esLimitacionDelServicio = codigo == CODIGO_DEMASIADOS_PEDIDOS || codigo >= CODIGO_ERROR_SERVIDOR_MINIMO,
                esRechazoDefinitivo = codigo in CODIGO_ERROR_CLIENTE_MINIMO until CODIGO_ERROR_SERVIDOR_MINIMO &&
                    codigo != CODIGO_DEMASIADOS_PEDIDOS
            )
        } catch (error: SerializationException) {
            throw ErrorElevacionException("El servicio de elevación devolvió una respuesta inválida", error)
        }
        if (respuesta.elevacion.size != lote.size) {
            throw ErrorElevacionException("El servicio de elevación devolvió datos incompletos")
        }
        return respuesta.elevacion
    }

    // Cinco decimales equivalen a cerca de un metro. La API pide punto decimal, por eso se usa Locale.US
    private fun formatearGrados(grados: Double): String {
        return String.format(Locale.US, "%.5f", grados)
    }

    private fun describirCodigoHttp(codigo: Int): String {
        return when {
            codigo == CODIGO_DEMASIADOS_PEDIDOS -> "El servicio de elevación recibió demasiados pedidos, probá más tarde"
            codigo >= CODIGO_ERROR_SERVIDOR_MINIMO -> "El servicio de elevación no está disponible (código $codigo)"
            else -> "El servicio de elevación rechazó el pedido (código $codigo)"
        }
    }
}
