package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.EstadoTrabajoSincronizacion
import com.unaj.senderos.core.domain.port.SolicitudSincronizacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FalsaSolicitudSincronizacion : SolicitudSincronizacion {

    val estado = MutableStateFlow(EstadoTrabajoSincronizacion.INACTIVO)
    val usuariosPedidos: MutableList<String> = mutableListOf()

    override fun sincronizarAhora(idUsuario: String) {
        usuariosPedidos.add(idUsuario)
    }

    override fun observarEstado(idUsuario: String): Flow<EstadoTrabajoSincronizacion> = estado
}
