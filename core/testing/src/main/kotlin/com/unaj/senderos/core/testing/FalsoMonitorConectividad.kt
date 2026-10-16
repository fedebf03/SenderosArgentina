package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.MonitorConectividad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class FalsoMonitorConectividad(hayConexionInicial: Boolean = true) : MonitorConectividad {

    private val conexion = MutableStateFlow(hayConexionInicial)

    var hayConexionActual: Boolean
        get() = conexion.value
        set(valor) {
            conexion.value = valor
        }

    override fun hayConexion(): Boolean = conexion.value

    override fun observarConexion(): Flow<Boolean> = conexion
}
