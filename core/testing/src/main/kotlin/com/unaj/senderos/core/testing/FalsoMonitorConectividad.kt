package com.unaj.senderos.falsos

import com.unaj.senderos.domain.repository.MonitorConectividad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class FalsoMonitorConectividad(hayConexionInicial: Boolean = true) : MonitorConectividad {

    private val conexion = MutableStateFlow(hayConexionInicial)

    var hayConexionActual: Boolean
        get() = conexion.value
        set(valor) {
            conexion.value = valor
        }

    // Devolver la conexión configurada por la prueba
    override fun hayConexion(): Boolean = conexion.value

    // Emitir la conexión actual y cada cambio que la prueba provoque
    override fun observarConexion(): Flow<Boolean> = conexion
}
