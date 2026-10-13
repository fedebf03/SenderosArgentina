package com.unaj.senderos.falsos

import com.unaj.senderos.domain.notificaciones.MensajeNotificacion
import com.unaj.senderos.domain.port.Notificador

class FalsoNotificador : Notificador {

    val mensajes: MutableList<MensajeNotificacion> = mutableListOf()
    var cantidadCancelacionesDeAvisoDeError = 0

    override fun notificar(mensaje: MensajeNotificacion) {
        mensajes.add(mensaje)
    }

    override fun cancelarAvisoDeError() {
        cantidadCancelacionesDeAvisoDeError++
    }
}
