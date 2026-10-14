package com.unaj.senderos.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// Respuesta de la API de elevación de Open-Meteo: una altura en metros por coordenada pedida
@Serializable
data class RespuestaElevacionOpenMeteo(
    @SerialName("elevation") val elevacion: List<Double> = emptyList()
)

// API REST de Open-Meteo (Copernicus DEM GLO-90); acepta hasta 100 coordenadas por pedido, en listas separadas por coma
interface ApiElevacionOpenMeteo {

    @GET("v1/elevation")
    suspend fun obtenerElevaciones(
        @Query("latitude", encoded = true) latitudes: String,
        @Query("longitude", encoded = true) longitudes: String
    ): RespuestaElevacionOpenMeteo
}
