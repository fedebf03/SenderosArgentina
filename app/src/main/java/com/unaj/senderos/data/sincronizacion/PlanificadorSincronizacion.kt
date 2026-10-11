package com.unaj.senderos.data.sincronizacion

interface PlanificadorSincronizacion {
    fun solicitarSincronizacion(idUsuario: String)

    fun cancelarSincronizacion(idUsuario: String)
}
