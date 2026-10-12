package com.unaj.senderos.falsos

import com.unaj.senderos.data.sincronizacion.PlanificadorSincronizacion

class FalsoPlanificadorSincronizacion : PlanificadorSincronizacion {

    val solicitudes: MutableList<String> = mutableListOf()
    val cancelaciones: MutableList<String> = mutableListOf()

    // Registrar la solicitud de sincronización del usuario
    override fun solicitarSincronizacion(idUsuario: String) {
        solicitudes.add(idUsuario)
    }

    // Registrar la cancelación de la sincronización del usuario
    override fun cancelarSincronizacion(idUsuario: String) {
        cancelaciones.add(idUsuario)
    }
}
