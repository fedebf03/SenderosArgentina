package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.network.recorrido.RecorridoFuenteRemota
import com.unaj.senderos.core.network.recorrido.RecorridoRed
class FalsoRecorridoFuenteRemota : RecorridoFuenteRemota {

    // Documentos remotos en memoria: usuario -> (id de recorrido -> documento)
    val documentosPorUsuario: MutableMap<String, MutableMap<String, RecorridoRed>> = mutableMapOf()

    var errorAlObtener: Throwable? = null
    val idsConErrorAlGuardar: MutableSet<String> = mutableSetOf()

    // Callback ejecutado durante guardarRecorrido para simular escrituras locales concurrentes
    var alGuardarRecorrido: (suspend (RecorridoRed) -> Unit)? = null

    val recorridosGuardados: MutableList<Pair<String, RecorridoRed>> = mutableListOf()
    val recorridosEliminados: MutableList<Pair<String, String>> = mutableListOf()
    val idsConErrorAlEliminar: MutableSet<String> = mutableSetOf()

    fun precargar(idUsuario: String, recorrido: RecorridoRed) {
        documentosPorUsuario.getOrPut(idUsuario) { mutableMapOf() }[recorrido.id] = recorrido
    }

    override suspend fun obtenerRecorridos(idUsuario: String): List<RecorridoRed> {
        errorAlObtener?.let { error -> throw error }
        return documentosPorUsuario[idUsuario]?.values?.toList() ?: emptyList()
    }

    // Callback ejecutado durante eliminarRecorrido para simular escrituras locales concurrentes
    var alEliminarRecorrido: (suspend () -> Unit)? = null

    override suspend fun eliminarRecorrido(idUsuario: String, idRecorrido: String) {
        recorridosEliminados.add(idUsuario to idRecorrido)
        alEliminarRecorrido?.invoke()
        if (idRecorrido in idsConErrorAlEliminar) throw IllegalStateException("Fallo al eliminar $idRecorrido")
        documentosPorUsuario[idUsuario]?.remove(idRecorrido)
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
