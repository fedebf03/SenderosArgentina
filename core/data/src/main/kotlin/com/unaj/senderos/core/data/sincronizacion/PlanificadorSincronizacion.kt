package com.unaj.senderos.core.data.sincronizacion

interface PlanificadorSincronizacion {
    fun solicitarSincronizacion(idUsuario: String)

    fun cancelarSincronizacion(idUsuario: String)
}
