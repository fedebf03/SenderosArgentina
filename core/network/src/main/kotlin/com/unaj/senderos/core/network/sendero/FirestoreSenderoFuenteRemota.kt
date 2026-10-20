package com.unaj.senderos.core.network.sendero

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

private const val COLECCION_USUARIOS = "usuarios"
private const val COLECCION_SENDEROS = "senderos"

class FirestoreSenderoFuenteRemota @Inject constructor(
    private val firestore: FirebaseFirestore
) : SenderoFuenteRemota {

    private fun documentoUsuario(idUsuario: String): DocumentReference {
        return firestore.collection(COLECCION_USUARIOS).document(idUsuario)
    }

    override suspend fun obtenerSenderos(idUsuario: String): List<SenderoRed> {
        val instantanea = documentoUsuario(idUsuario).collection(COLECCION_SENDEROS).get(Source.SERVER).await()
        return instantanea.documents.mapNotNull { documento -> documento.toObject(SenderoRed::class.java) }
    }

    // RF7 - Sincronización: cada sendero se sube completo como un documento dentro del usuario
    override suspend fun guardarSendero(idUsuario: String, sendero: SenderoRed) {
        documentoUsuario(idUsuario).collection(COLECCION_SENDEROS).document(sendero.id).set(sendero).await()
    }

}
