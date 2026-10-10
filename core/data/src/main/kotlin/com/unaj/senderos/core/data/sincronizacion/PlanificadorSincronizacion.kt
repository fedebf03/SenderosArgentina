package com.unaj.senderos.data.sincronizacion

// Punto de entrada para pedir que se sincronicen los senderos de un usuario
interface PlanificadorSincronizacion {
    // Solicitar la sincronización diferida de los senderos del usuario
    fun solicitarSincronizacion(idUsuario: String)

    // Cancelar la sincronización encolada o en curso del usuario
    fun cancelarSincronizacion(idUsuario: String)
}
