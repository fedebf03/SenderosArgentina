package com.unaj.senderos.core.network

// Perfil del usuario como se guarda en Firestore, dentro del campo "perfil" del documento del usuario
data class PerfilRed(
    val nombre: String = "",
    val pais: String = "",
    val provincia: String = "",
    val localidad: String = "",
    val foto: String = ""
)
