package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.Flow

// Situación del trabajo que sube y baja los datos del usuario
// EN_COLA: pedida y por arrancar (o sin red); EN_REINTENTO: ya falló un intento y espera para volver a probar
enum class EstadoTrabajoSincronizacion { INACTIVO, EN_COLA, EN_REINTENTO, EN_CURSO }

interface SolicitudSincronizacion {

    // Pedir una sincronización ya, sin esperar a la próxima apertura de la app; con red se ejecuta enseguida y sin red queda en cola
    fun sincronizarAhora(idUsuario: String)

    // Observar si la sincronización del usuario está inactiva, esperando conexión o en curso
    fun observarEstado(idUsuario: String): Flow<EstadoTrabajoSincronizacion>
}
