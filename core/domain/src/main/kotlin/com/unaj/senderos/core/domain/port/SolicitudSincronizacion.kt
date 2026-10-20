package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.Flow

// EN_COLA está pedida y por arrancar (o sin red). EN_REINTENTO ya falló un intento y espera para volver a probar
enum class EstadoTrabajoSincronizacion { INACTIVO, EN_COLA, EN_REINTENTO, EN_CURSO }

interface SolicitudSincronizacion {

    // No espera a la próxima apertura de la app. Con red se ejecuta enseguida y sin red queda en cola
    fun sincronizarAhora(idUsuario: String)

    fun observarEstado(idUsuario: String): Flow<EstadoTrabajoSincronizacion>
}
