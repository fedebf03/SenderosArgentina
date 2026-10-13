package com.unaj.senderos.falsos

import com.unaj.senderos.data.remote.RecorridoFuenteRemota
import com.unaj.senderos.data.remote.RecorridoRed

class FalsoRecorridoFuenteRemota : RecorridoFuenteRemota {

    // Documentos remotos en memoria: usuario -> (id de recorrido -> documento)
    val documentosPorUsuario: MutableMap<String, MutableMap<String, RecorridoRed>> = mutableMapOf()

    var errorAlObtener: Throwable? = null
    val idsConErrorAlGuardar: MutableSet<String> = mutableSetOf()

    // Callback ejecutado durante guardarRecorrido para simular escrituras locales concurrentes
    var alGuardarRecorrido: (suspend (RecorridoRed) -> Unit)? = null

    val recorridosGuardados: MutableList<Pair<String, RecorridoRed>> = mutableListOf()

    fun precargar(idUsuario: String, recorrido: RecorridoRed) {
        documentosPorUsuario.getOrPut(idUsuario) { mutableMapOf() }[recorrido.id] = recorrido
    }

    override suspend fun obtenerRecorridos(idUsuario: String): List<RecorridoRed> {
        errorAlObtener?.let { error -> throw error }
        return documentosPorUsuario[idUsuario]?.values?.toList() ?: emptyList()
    }

    override suspend fun guardarRecorrido(idUsuario: String, recorrido: RecorridoRed) {
        recorridosGuardados.add(idUsuario to recorrido)
        alGuardarRecorrido?.invoke(recorrido)
        if (recorrido.id in idsConErrorAlGuardar) {
            throw IllegalStateException("Fallo al guardar ${recorrido.id}")
        }
        documentosPorUsuario.getOrPut(idUsuario) { mutableMapOf() }[recorrido.id] = recorrido
    }
}
