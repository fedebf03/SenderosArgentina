package com.unaj.senderos.core.network

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

private const val COLECCION_USUARIOS = "usuarios"
private const val COLECCION_RECORRIDOS = "recorridos"

class FirestoreRecorridoFuenteRemota @Inject constructor(
    private val firestore: FirebaseFirestore
) : RecorridoFuenteRemota {

    private fun documentoUsuario(idUsuario: String): DocumentReference {
        return firestore.collection(COLECCION_USUARIOS).document(idUsuario)
    }

    override suspend fun obtenerRecorridos(idUsuario: String): List<RecorridoRed> {
        val instantanea = documentoUsuario(idUsuario).collection(COLECCION_RECORRIDOS).get(Source.SERVER).await()
        return instantanea.documents.mapNotNull { documento -> documento.toObject(RecorridoRed::class.java) }
    }

    override suspend fun guardarRecorrido(idUsuario: String, recorrido: RecorridoRed) {
        documentoUsuario(idUsuario).collection(COLECCION_RECORRIDOS).document(recorrido.id).set(recorrido).await()
    }

    override suspend fun eliminarRecorrido(idUsuario: String, idRecorrido: String) {
        documentoUsuario(idUsuario).collection(COLECCION_RECORRIDOS).document(idRecorrido).delete().await()
    }
}
