package com.unaj.senderos.falsos

import com.unaj.senderos.data.sincronizacion.PlanificadorSincronizacion

class FalsoPlanificadorSincronizacion : PlanificadorSincronizacion {

    val solicitudes: MutableList<String> = mutableListOf()
    val cancelaciones: MutableList<String> = mutableListOf()

    override fun solicitarSincronizacion(idUsuario: String) {
        solicitudes.add(idUsuario)
    }

    override fun cancelarSincronizacion(idUsuario: String) {
        cancelaciones.add(idUsuario)
    }
}
