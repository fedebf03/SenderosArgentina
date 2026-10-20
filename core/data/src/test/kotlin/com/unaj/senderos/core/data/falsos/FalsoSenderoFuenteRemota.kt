package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.network.sendero.SenderoFuenteRemota
import com.unaj.senderos.core.network.sendero.SenderoRed
class FalsoSenderoFuenteRemota : SenderoFuenteRemota {

    // Documentos remotos en memoria: usuario -> (id de sendero -> documento)
    val documentosPorUsuario: MutableMap<String, MutableMap<String, SenderoRed>> = mutableMapOf()

    var errorAlObtener: Throwable? = null
    val idsConErrorAlGuardar: MutableSet<String> = mutableSetOf()
    val erroresPorIdAlGuardar: MutableMap<String, Throwable> = mutableMapOf()

    // Callback ejecutado durante guardarSendero para simular escrituras locales concurrentes
    var alGuardarSendero: (suspend (SenderoRed) -> Unit)? = null

    val usuariosConsultados: MutableList<String> = mutableListOf()
    val senderosGuardados: MutableList<Pair<String, SenderoRed>> = mutableListOf()

    fun precargar(idUsuario: String, sendero: SenderoRed) {
        documentosPorUsuario.getOrPut(idUsuario) { mutableMapOf() }[sendero.id] = sendero
    }

    override suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed> {
        usuariosConsultados.add(idUsuario)
        errorAlObtener?.let { error -> throw error }
        return documentosPorUsuario[idUsuario]?.values?.toList() ?: emptyList()
    }

    override suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed) {
        senderosGuardados.add(idUsuario to sendero)
        alGuardarSendero?.invoke(sendero)
        erroresPorIdAlGuardar[sendero.id]?.let { error -> throw error }
        if (sendero.id in idsConErrorAlGuardar) {
            throw IllegalStateException("Fallo al guardar ${sendero.id}")
        }
        documentosPorUsuario.getOrPut(idUsuario) { mutableMapOf() }[sendero.id] = sendero
    }

}
