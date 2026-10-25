package com.unaj.senderos.core.network.perfil

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

private const val COLECCION_USUARIOS = "usuarios"
private const val CAMPO_PERFIL = "perfil"

// RF2 - Perfil: se guarda con merge en el campo "perfil" del usuario para no pisar el resto del documento
class FirestorePerfilFuenteRemota @Inject constructor(
    private val firestore: FirebaseFirestore
) : PerfilFuenteRemota {

    override suspend fun obtenerPerfil(idUsuario: String): PerfilRed? {
        val documento = firestore.collection(COLECCION_USUARIOS).document(idUsuario).get(Source.SERVER).await()
        return documento.get(CAMPO_PERFIL, PerfilRed::class.java)
    }

    override suspend fun guardarPerfil(idUsuario: String, perfil: PerfilRed) {
        firestore.collection(COLECCION_USUARIOS).document(idUsuario)
            .set(mapOf(CAMPO_PERFIL to perfil), SetOptions.merge())
            .await()
    }
}
