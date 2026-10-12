package com.unaj.senderos.domain.repository

interface MonitorConectividad {
    // Indicar si el dispositivo tiene en este momento una red activa con salida a Internet
    fun hayConexion(): Boolean
}
