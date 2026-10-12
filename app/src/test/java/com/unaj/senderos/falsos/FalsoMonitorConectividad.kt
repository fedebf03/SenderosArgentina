package com.unaj.senderos.falsos

import com.unaj.senderos.domain.repository.MonitorConectividad

class FalsoMonitorConectividad(var hayConexionActual: Boolean = true) : MonitorConectividad {

    // Devolver la conexión configurada por la prueba
    override fun hayConexion(): Boolean = hayConexionActual
}
