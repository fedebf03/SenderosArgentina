package com.unaj.senderos.domain.notificaciones

// Mensaje que la aplicación puede mostrar al usuario como notificación tras sincronizar
sealed interface MensajeNotificacion {
    data class SenderosSubidos(val cantidad: Int) : MensajeNotificacion
    data class SenderosConError(val cantidad: Int) : MensajeNotificacion
}
