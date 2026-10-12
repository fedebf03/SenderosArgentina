package com.unaj.senderos.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ConvertidoresSendero {

    private val json = Json

    // Convertir una lista de textos en JSON para guardarla en una columna de Room
    @TypeConverter
    fun listaTextosATexto(lista: List<String>): String {
        return json.encodeToString(ListSerializer(String.serializer()), lista)
    }

    // Recuperar la lista de textos desde su JSON guardado en Room
    @TypeConverter
    fun textoAListaTextos(texto: String): List<String> {
        return json.decodeFromString(ListSerializer(String.serializer()), texto)
    }

    // Convertir la lista de etapas en JSON para guardarla en una columna de Room
    @TypeConverter
    fun listaEtapasATexto(lista: List<EtapaEntidad>): String {
        return json.encodeToString(ListSerializer(EtapaEntidad.serializer()), lista)
    }

    // Recuperar la lista de etapas desde su JSON guardado en Room
    @TypeConverter
    fun textoAListaEtapas(texto: String): List<EtapaEntidad> {
        return json.decodeFromString(ListSerializer(EtapaEntidad.serializer()), texto)
    }

    // Convertir la lista de puntos del recorrido en JSON para guardarla en una columna de Room
    @TypeConverter
    fun listaPuntosATexto(lista: List<PuntoRecorridoEntidad>): String {
        return json.encodeToString(ListSerializer(PuntoRecorridoEntidad.serializer()), lista)
    }

    // Recuperar la lista de puntos del recorrido desde su JSON guardado en Room
    @TypeConverter
    fun textoAListaPuntos(texto: String): List<PuntoRecorridoEntidad> {
        return json.decodeFromString(ListSerializer(PuntoRecorridoEntidad.serializer()), texto)
    }

    // Convertir el trazado caminado en JSON para guardarlo en una columna de Room
    @TypeConverter
    fun listaCoordenadasATexto(lista: List<CoordenadaEntidad>): String {
        return json.encodeToString(ListSerializer(CoordenadaEntidad.serializer()), lista)
    }

    // Recuperar el trazado caminado desde su JSON guardado en Room
    @TypeConverter
    fun textoAListaCoordenadas(texto: String): List<CoordenadaEntidad> {
        return json.decodeFromString(ListSerializer(CoordenadaEntidad.serializer()), texto)
    }
}
