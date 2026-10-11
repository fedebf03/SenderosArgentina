package com.unaj.senderos.falsos

import com.unaj.senderos.data.sincronizacion.PlanificadorSincronizacion

class FalsoPlanificadorSincronizacion : PlanificadorSincronizacion {

    val idsUsuarioSolicitados: MutableList<String> = mutableListOf()
    val idsUsuarioCancelados: MutableList<String> = mutableListOf()

    override fun solicitarSincronizacion(idUsuario: String) {
        idsUsuarioSolicitados.add(idUsuario)
    }

    override fun cancelarSincronizacion(idUsuario: String) {
        idsUsuarioCancelados.add(idUsuario)
    }
}
