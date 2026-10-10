package com.unaj.senderos.falsos

import com.unaj.senderos.data.remote.SenderoFuenteRemota
import com.unaj.senderos.data.remote.SenderoRed

class FalsoSenderoFuenteRemota : SenderoFuenteRemota {

    // Documentos remotos en memoria: usuario -> (id de sendero -> documento)
    val documentosPorUsuario: MutableMap<String, MutableMap<String, SenderoRed>> = mutableMapOf()
    val usuariosConMarcaEjemplos: MutableSet<String> = mutableSetOf()

    var errorAlObtener: Throwable? = null
    var errorAlObtenerMarca: Throwable? = null
    val idsConErrorAlGuardar: MutableSet<String> = mutableSetOf()
    val erroresPorIdAlGuardar: MutableMap<String, Throwable> = mutableMapOf()

    // Callback ejecutado durante guardarSendero para simular escrituras locales concurrentes
    var alGuardarSendero: (suspend (SenderoRed) -> Unit)? = null

    val usuariosConsultados: MutableList<String> = mutableListOf()
    val senderosGuardados: MutableList<Pair<String, SenderoRed>> = mutableListOf()
    val usuariosConMarcaRegistrada: MutableList<String> = mutableListOf()

    // Agregar un documento remoto precargado para el usuario indicado
    fun precargar(idUsuario: String, sendero: SenderoRed) {
        documentosPorUsuario.getOrPut(idUsuario) { mutableMapOf() }[sendero.id] = sendero
    }

    // Obtener los documentos remotos del usuario o lanzar el error configurado
    override suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed> {
        usuariosConsultados.add(idUsuario)
        errorAlObtener?.let { error -> throw error }
        return documentosPorUsuario[idUsuario]?.values?.toList() ?: emptyList()
    }

    // Guardar el documento en memoria o lanzar el error si el id está configurado para fallar
    override suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed) {
        senderosGuardados.add(idUsuario to sendero)
        alGuardarSendero?.invoke(sendero)
        erroresPorIdAlGuardar[sendero.id]?.let { error -> throw error }
        if (sendero.id in idsConErrorAlGuardar) {
            throw IllegalStateException("Fallo al guardar ${sendero.id}")
        }
        documentosPorUsuario.getOrPut(idUsuario) { mutableMapOf() }[sendero.id] = sendero
    }

    // Consultar la marca de ejemplos cargados o lanzar el error configurado
    override suspend fun obtenerMarcaEjemplosCargados(idUsuario: String): Boolean {
        errorAlObtenerMarca?.let { error -> throw error }
        return idUsuario in usuariosConMarcaEjemplos
    }

    // Registrar la marca de ejemplos cargados del usuario
    override suspend fun registrarMarcaEjemplosCargados(idUsuario: String) {
        usuariosConMarcaRegistrada.add(idUsuario)
        usuariosConMarcaEjemplos.add(idUsuario)
    }
}
