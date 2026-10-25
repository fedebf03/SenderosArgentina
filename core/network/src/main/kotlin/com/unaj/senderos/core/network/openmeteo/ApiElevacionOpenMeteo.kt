package com.unaj.senderos.core.network.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class RespuestaElevacionOpenMeteo(
    @SerialName("elevation") val elevacion: List<Double> = emptyList()
)

// RF3 - Detalle: las alturas del perfil salen del modelo Copernicus GLO-90, con hasta 100 coordenadas por pedido
interface ApiElevacionOpenMeteo {

    @GET("v1/elevation")
    suspend fun obtenerElevaciones(
        @Query("latitude", encoded = true) latitudes: String,
        @Query("longitude", encoded = true) longitudes: String
    ): RespuestaElevacionOpenMeteo
}
