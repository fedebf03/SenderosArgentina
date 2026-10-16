package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.Flow

interface MonitorConectividad {
    // Indicar si el dispositivo tiene en este momento una red activa con salida a Internet
    fun hayConexion(): Boolean

    // Observar la conexión como flujo compartido: emite el valor actual y luego solo cuando cambia
    fun observarConexion(): Flow<Boolean>
}
