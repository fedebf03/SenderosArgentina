package com.unaj.senderos.falsos

import com.unaj.senderos.data.sincronizacion.PlanificadorSincronizacion

class FalsoPlanificadorSincronizacion : PlanificadorSincronizacion {

    val idsUsuarioSolicitados: MutableList<String> = mutableListOf()
    val idsUsuarioCancelados: MutableList<String> = mutableListOf()

    // Registrar el identificador del usuario cuya sincronización fue solicitada
    override fun solicitarSincronizacion(idUsuario: String) {
        idsUsuarioSolicitados.add(idUsuario)
    }

    // Registrar el identificador del usuario cuya sincronización fue cancelada
    override fun cancelarSincronizacion(idUsuario: String) {
        idsUsuarioCancelados.add(idUsuario)
    }
}
