package com.unaj.senderos.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// Convertidores de Room para guardar listas como texto JSON
class ConvertidoresSendero {

    private val json = Json

    // Convertir una lista de textos a JSON
    @TypeConverter
    fun listaTextosATexto(lista: List<String>): String {
        return json.encodeToString(ListSerializer(String.serializer()), lista)
    }

    // Convertir un JSON a lista de textos
    @TypeConverter
    fun textoAListaTextos(texto: String): List<String> {
        return json.decodeFromString(ListSerializer(String.serializer()), texto)
    }

    // Convertir una lista de etapas a JSON
    @TypeConverter
    fun listaEtapasATexto(lista: List<EtapaEntidad>): String {
        return json.encodeToString(ListSerializer(EtapaEntidad.serializer()), lista)
    }

    // Convertir un JSON a lista de etapas
    @TypeConverter
    fun textoAListaEtapas(texto: String): List<EtapaEntidad> {
        return json.decodeFromString(ListSerializer(EtapaEntidad.serializer()), texto)
    }
}
