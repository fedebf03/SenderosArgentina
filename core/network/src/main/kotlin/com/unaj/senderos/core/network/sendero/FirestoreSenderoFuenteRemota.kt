package com.unaj.senderos.data.remote

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

private const val COLECCION_USUARIOS = "usuarios"
private const val COLECCION_SENDEROS = "senderos"

class FirestoreSenderoFuenteRemota(
    private val firestore: FirebaseFirestore
) : SenderoFuenteRemota {

    private fun documentoUsuario(idUsuario: String): DocumentReference {
        return firestore.collection(COLECCION_USUARIOS).document(idUsuario)
    }

    override suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed> {
        val instantanea = documentoUsuario(idUsuario).collection(COLECCION_SENDEROS).get(Source.SERVER).await()
        return instantanea.documents.mapNotNull { documento -> documento.toObject(SenderoRed::class.java) }
    }

    override suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed) {
        documentoUsuario(idUsuario).collection(COLECCION_SENDEROS).document(sendero.id).set(sendero).await()
    }

}
