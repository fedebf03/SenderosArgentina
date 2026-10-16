package com.unaj.senderos.core.network

import com.google.firebase.firestore.FirebaseFirestoreException

enum class TipoErrorRemoto {
    SIN_PERMISO,
    SIN_RESPUESTA,
    TRANSITORIO,
    PERMANENTE,
    DESCONOCIDO
}

// Las capas de arriba deciden con este tipo sin conocer las excepciones de Firestore
fun Throwable?.tipoErrorRemoto(): TipoErrorRemoto {
    if (this !is FirebaseFirestoreException) return TipoErrorRemoto.DESCONOCIDO
    return when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> TipoErrorRemoto.SIN_PERMISO
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> TipoErrorRemoto.SIN_RESPUESTA
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> TipoErrorRemoto.TRANSITORIO
        else -> TipoErrorRemoto.PERMANENTE
    }
}
