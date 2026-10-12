package com.unaj.senderos.data.sincronizacion

interface PlanificadorSincronizacion {
    // Solicitar la sincronización de los senderos del usuario
    fun solicitarSincronizacion(idUsuario: String)

    // Cancelar la sincronización de los senderos del usuario
    fun cancelarSincronizacion(idUsuario: String)
}
