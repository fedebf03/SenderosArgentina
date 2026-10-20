package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.Flow

interface MonitorConectividad {
    fun hayConexion(): Boolean

    // Emite el valor actual y después solo cuando cambia
    fun observarConexion(): Flow<Boolean>
}
