package com.unaj.senderos.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val COLECCION_PUBLICOS = "senderosPublicos"

class FirestoreSenderoPublicoFuenteRemota(
    private val firestore: FirebaseFirestore
) : SenderoPublicoFuenteRemota {

    override fun observarTodos(): Flow<List<SenderoPublicoRed>> = callbackFlow {
        val registro = firestore.collection(COLECCION_PUBLICOS)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
            .addSnapshotListener { instantanea, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(instantanea?.documents.orEmpty().mapNotNull { documento -> documento.aSenderoPublicoRed() })
            }
        awaitClose { registro.remove() }
    }

    override fun observarUno(idSendero: String): Flow<SenderoPublicoRed?> = callbackFlow {
        val registro = firestore.collection(COLECCION_PUBLICOS).document(idSendero)
            .addSnapshotListener { documento, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(documento?.aSenderoPublicoRed())
            }
        awaitClose { registro.remove() }
    }

    override suspend fun guardar(sendero: SenderoPublicoRed) {
        firestore.collection(COLECCION_PUBLICOS).document(sendero.sendero.id).set(sendero).await()
    }

    override suspend fun eliminar(idSendero: String) {
        firestore.collection(COLECCION_PUBLICOS).document(idSendero).delete().await()
    }

    private fun DocumentSnapshot.aSenderoPublicoRed(): SenderoPublicoRed? {
        return runCatching { toObject(SenderoPublicoRed::class.java) }.getOrNull()
    }
}
