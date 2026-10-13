package com.unaj.senderos.domain.notificaciones

sealed interface MensajeNotificacion {
    data class SenderosSubidos(val cantidad: Int) : MensajeNotificacion
    data class SenderosConError(val cantidad: Int) : MensajeNotificacion
}
