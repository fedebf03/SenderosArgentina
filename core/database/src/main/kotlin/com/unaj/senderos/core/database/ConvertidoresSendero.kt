package com.unaj.senderos.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import com.unaj.senderos.core.database.recorrido.CoordenadaEntidad
import com.unaj.senderos.core.database.sendero.EtapaEntidad
import com.unaj.senderos.core.database.sendero.PerfilElevacionEntidad
import com.unaj.senderos.core.database.sendero.PuntoRecorridoEntidad

class ConvertidoresSendero {

    private val json = Json

    @TypeConverter
    fun listaTextosATexto(lista: List<String>): String {
        return json.encodeToString(ListSerializer(String.serializer()), lista)
    }

    @TypeConverter
    fun textoAListaTextos(texto: String): List<String> {
        return json.decodeFromString(ListSerializer(String.serializer()), texto)
    }

    @TypeConverter
    fun listaEtapasATexto(lista: List<EtapaEntidad>): String {
        return json.encodeToString(ListSerializer(EtapaEntidad.serializer()), lista)
    }

    @TypeConverter
    fun textoAListaEtapas(texto: String): List<EtapaEntidad> {
        return json.decodeFromString(ListSerializer(EtapaEntidad.serializer()), texto)
    }

    @TypeConverter
    fun listaPuntosATexto(lista: List<PuntoRecorridoEntidad>): String {
        return json.encodeToString(ListSerializer(PuntoRecorridoEntidad.serializer()), lista)
    }

    @TypeConverter
    fun textoAListaPuntos(texto: String): List<PuntoRecorridoEntidad> {
        return json.decodeFromString(ListSerializer(PuntoRecorridoEntidad.serializer()), texto)
    }

    @TypeConverter
    fun perfilATexto(perfil: PerfilElevacionEntidad?): String? {
        return perfil?.let { valor -> json.encodeToString(PerfilElevacionEntidad.serializer(), valor) }
    }

    // Un texto ilegible se trata como un perfil sin calcular
    @TypeConverter
    fun textoAPerfil(texto: String?): PerfilElevacionEntidad? {
        if (texto == null) return null
        return try {
            json.decodeFromString(PerfilElevacionEntidad.serializer(), texto)
        } catch (error: SerializationException) {
            null
        }
    }

    @TypeConverter
    fun listaCoordenadasATexto(lista: List<CoordenadaEntidad>): String {
        return json.encodeToString(ListSerializer(CoordenadaEntidad.serializer()), lista)
    }

    @TypeConverter
    fun textoAListaCoordenadas(texto: String): List<CoordenadaEntidad> {
        return json.decodeFromString(ListSerializer(CoordenadaEntidad.serializer()), texto)
    }
}
