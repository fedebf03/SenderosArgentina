package com.unaj.senderos.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// Clima de ahora que informa Open-Meteo; esDeDia es 1 de día y 0 de noche
@Serializable
data class ClimaActualOpenMeteo(
    @SerialName("temperature_2m") val temperatura: Double,
    @SerialName("apparent_temperature") val sensacionTermica: Double? = null,
    @SerialName("wind_speed_10m") val viento: Double,
    @SerialName("wind_gusts_10m") val rafagas: Double? = null,
    @SerialName("weather_code") val codigoClima: Int,
    @SerialName("is_day") val esDeDia: Int = 1
)

// Pronóstico diario: una lista por dato, con una posición por día; cualquier valor puede venir nulo
@Serializable
data class ClimaDiarioOpenMeteo(
    @SerialName("time") val fechas: List<String> = emptyList(),
    @SerialName("weather_code") val codigoClima: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperaturaMaxima: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperaturaMinima: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitacion: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val probabilidadLluvia: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m_max") val vientoMaximo: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m_max") val rafagasMaximas: List<Double?> = emptyList()
)

@Serializable
data class RespuestaPronosticoOpenMeteo(
    @SerialName("current") val actual: ClimaActualOpenMeteo? = null,
    @SerialName("daily") val diario: ClimaDiarioOpenMeteo? = null
)

// API REST de pronóstico de Open-Meteo: clima de ahora y cinco días, con el viento en km/h y la zona horaria del lugar
interface ApiPronosticoOpenMeteo {

    @GET(
        "v1/forecast?current=temperature_2m,apparent_temperature,wind_speed_10m,wind_gusts_10m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max," +
            "wind_speed_10m_max,wind_gusts_10m_max&timezone=auto&forecast_days=5&wind_speed_unit=kmh"
    )
    suspend fun obtenerPronostico(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double
    ): RespuestaPronosticoOpenMeteo
}
