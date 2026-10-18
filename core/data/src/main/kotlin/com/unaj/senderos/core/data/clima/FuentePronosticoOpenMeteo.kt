package com.unaj.senderos.core.data.clima

import android.util.Log
import com.unaj.senderos.core.domain.clima.ClimaActual
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.clima.PronosticoClima
import com.unaj.senderos.core.domain.clima.PronosticoDia
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.ErrorPronosticoException
import com.unaj.senderos.core.domain.port.FuentePronostico
import com.unaj.senderos.core.domain.port.Reloj
import com.unaj.senderos.core.domain.port.SinConexionPronosticoException
import com.unaj.senderos.core.network.ApiPronosticoOpenMeteo
import com.unaj.senderos.core.network.RespuestaPronosticoOpenMeteo
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "CLIMA"
private const val CODIGO_DEMASIADOS_PEDIDOS = 429
private const val CODIGO_ERROR_SERVIDOR_MINIMO = 500
private const val MENSAJE_DATOS_INCOMPLETOS = "El servicio del clima devolvió datos incompletos"

// Fuente del pronóstico que consulta Open-Meteo y traduce sus fallos a mensajes en español
class FuentePronosticoOpenMeteo @Inject constructor(
    private val api: ApiPronosticoOpenMeteo,
    private val reloj: Reloj
) : FuentePronostico {

    // Pedir el pronóstico del lugar y convertirlo al dominio; un fallo de red, de servicio o de datos vuelve como failure con un mensaje claro
    override suspend fun obtener(lugar: Coordenada): Result<PronosticoClima> {
        return try {
            Log.d(TAG, "Pidiendo el clima a Open-Meteo")
            val respuesta = api.obtenerPronostico(lugar.latitud, lugar.longitud)
            Result.success(traducir(lugar, respuesta))
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: ErrorPronosticoException) {
            Log.e(TAG, "No se pudo obtener el clima: ${error.message}", error)
            Result.failure(error)
        } catch (error: IOException) {
            Log.e(TAG, "Sin conexión para consultar el clima", error)
            Result.failure(SinConexionPronosticoException(error))
        } catch (error: HttpException) {
            Log.e(TAG, "El servicio del clima respondió ${error.code()}", error)
            Result.failure(ErrorPronosticoException(describirCodigoHttp(error.code()), error))
        } catch (error: SerializationException) {
            Log.e(TAG, "Respuesta del clima inválida", error)
            Result.failure(ErrorPronosticoException("El servicio del clima devolvió una respuesta inválida", error))
        } catch (error: Exception) {
            Log.e(TAG, "Fallo inesperado al consultar el clima", error)
            Result.failure(ErrorPronosticoException("No se pudo consultar el clima", error))
        }
    }

    // Traducir la respuesta validando que traiga el clima de ahora y una posición por día en cada lista
    private fun traducir(lugar: Coordenada, respuesta: RespuestaPronosticoOpenMeteo): PronosticoClima {
        val actualRed = respuesta.actual ?: throw ErrorPronosticoException(MENSAJE_DATOS_INCOMPLETOS)
        val diario = respuesta.diario ?: throw ErrorPronosticoException(MENSAJE_DATOS_INCOMPLETOS)
        val cantidad = diario.fechas.size
        val largos = listOf(
            diario.codigoClima.size, diario.temperaturaMaxima.size, diario.temperaturaMinima.size, diario.precipitacion.size,
            diario.probabilidadLluvia.size, diario.vientoMaximo.size, diario.rafagasMaximas.size
        )
        if (largos.any { largo -> largo != cantidad }) throw ErrorPronosticoException(MENSAJE_DATOS_INCOMPLETOS)
        val dias = List(cantidad) { indice ->
            PronosticoDia(
                fecha = diario.fechas[indice],
                temperaturaMaximaCelsius = diario.temperaturaMaxima[indice] ?: throw ErrorPronosticoException(MENSAJE_DATOS_INCOMPLETOS),
                temperaturaMinimaCelsius = diario.temperaturaMinima[indice] ?: throw ErrorPronosticoException(MENSAJE_DATOS_INCOMPLETOS),
                probabilidadLluviaPorcentaje = diario.probabilidadLluvia[indice],
                // Un dato numérico ausente cuenta como cero para no inventar avisos
                precipitacionMilimetros = diario.precipitacion[indice] ?: 0.0,
                vientoMaximoKilometrosPorHora = diario.vientoMaximo[indice] ?: 0.0,
                rafagasMaximasKilometrosPorHora = diario.rafagasMaximas[indice] ?: 0.0,
                condicion = CondicionClima.desdeCodigo(diario.codigoClima[indice] ?: -1)
            )
        }
        return PronosticoClima(
            lugar = lugar,
            obtenidoMilisegundos = reloj(),
            actual = ClimaActual(
                temperaturaCelsius = actualRed.temperatura,
                sensacionTermicaCelsius = actualRed.sensacionTermica,
                vientoKilometrosPorHora = actualRed.viento,
                rafagasKilometrosPorHora = actualRed.rafagas,
                condicion = CondicionClima.desdeCodigo(actualRed.codigoClima),
                esDeDia = actualRed.esDeDia == 1
            ),
            dias = dias
        )
    }

    // Describir en español un código de estado HTTP de error
    private fun describirCodigoHttp(codigo: Int): String {
        return when {
            codigo == CODIGO_DEMASIADOS_PEDIDOS -> "El servicio del clima recibió demasiados pedidos, probá más tarde"
            codigo >= CODIGO_ERROR_SERVIDOR_MINIMO -> "El servicio del clima no está disponible (código $codigo)"
            else -> "El servicio del clima rechazó el pedido (código $codigo)"
        }
    }
}
