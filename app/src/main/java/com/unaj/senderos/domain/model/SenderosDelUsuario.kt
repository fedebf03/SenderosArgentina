package com.unaj.senderos.domain.model

// Resultado inmutable que agrupa al usuario con sesión activa y sus senderos
data class SenderosDelUsuario(
    val usuario: Usuario,
    val senderos: List<Sendero>
)
