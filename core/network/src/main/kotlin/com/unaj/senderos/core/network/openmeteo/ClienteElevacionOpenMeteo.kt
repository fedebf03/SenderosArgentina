package com.unaj.senderos.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val URL_BASE_OPEN_METEO = "https://api.open-meteo.com/"
private const val SEGUNDOS_CONEXION = 10L
private const val SEGUNDOS_LECTURA = 15L
private const val SEGUNDOS_TOTAL = 30L

// Crear el cliente Retrofit de Open-Meteo con tiempos de espera acotados para no colgar la sincronización ni la pantalla
internal fun crearRetrofitOpenMeteo(): Retrofit {
    val cliente = OkHttpClient.Builder()
        .connectTimeout(SEGUNDOS_CONEXION, TimeUnit.SECONDS)
        .readTimeout(SEGUNDOS_LECTURA, TimeUnit.SECONDS)
        .writeTimeout(SEGUNDOS_LECTURA, TimeUnit.SECONDS)
        .callTimeout(SEGUNDOS_TOTAL, TimeUnit.SECONDS)
        .build()
    val json = Json { ignoreUnknownKeys = true }
    return Retrofit.Builder()
        .baseUrl(URL_BASE_OPEN_METEO)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

// Crear la API de elevación de Open-Meteo
fun crearApiElevacionOpenMeteo(): ApiElevacionOpenMeteo = crearRetrofitOpenMeteo().create(ApiElevacionOpenMeteo::class.java)

// Crear la API del pronóstico de Open-Meteo
fun crearApiPronosticoOpenMeteo(): ApiPronosticoOpenMeteo = crearRetrofitOpenMeteo().create(ApiPronosticoOpenMeteo::class.java)
