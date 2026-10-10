package com.unaj.senderos.data.remote

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

private const val COLECCION_USUARIOS = "usuarios"
private const val COLECCION_SENDEROS = "senderos"
private const val CAMPO_EJEMPLOS_CARGADOS = "ejemplosCargados"

// Implementación sobre Firestore con datos aislados bajo /usuarios/{idUsuario}
class FirestoreSenderoFuenteRemota(
    private val firestore: FirebaseFirestore
) : SenderoFuenteRemota {

    // Obtener el documento raíz del usuario
    private fun documentoUsuario(idUsuario: String): DocumentReference {
        return firestore.collection(COLECCION_USUARIOS).document(idUsuario)
    }

    // Obtener todos los senderos del usuario desde Firestore
    override suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed> {
        val instantanea = documentoUsuario(idUsuario).collection(COLECCION_SENDEROS).get(Source.SERVER).await()
        return instantanea.documents.mapNotNull { documento -> documento.toObject(SenderoRed::class.java) }
    }

    // Escribir el documento del sendero reemplazando el contenido anterior
    override suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed) {
        documentoUsuario(idUsuario).collection(COLECCION_SENDEROS).document(sendero.id).set(sendero).await()
    }

    // Leer la marca de ejemplos cargados; false si el documento no existe
    override suspend fun obtenerMarcaEjemplosCargados(idUsuario: String): Boolean {
        val documento = documentoUsuario(idUsuario).get(Source.SERVER).await()
        return documento.getBoolean(CAMPO_EJEMPLOS_CARGADOS) ?: false
    }

    // Escribir la marca de ejemplos cargados fusionando con el documento existente
    override suspend fun registrarMarcaEjemplosCargados(idUsuario: String) {
        documentoUsuario(idUsuario)
            .set(mapOf(CAMPO_EJEMPLOS_CARGADOS to true), SetOptions.merge())
            .await()
    }
}
