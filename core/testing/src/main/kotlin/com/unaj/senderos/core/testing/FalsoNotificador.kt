package com.unaj.senderos.falsos

import com.unaj.senderos.domain.notificaciones.MensajeNotificacion
import com.unaj.senderos.domain.port.Notificador

class FalsoNotificador : Notificador {

    val mensajes: MutableList<MensajeNotificacion> = mutableListOf()
    var cantidadCancelacionesDeAvisoDeError = 0

    // Registrar el mensaje que se pidió mostrar
    override fun notificar(mensaje: MensajeNotificacion) {
        mensajes.add(mensaje)
    }

    // Contar las cancelaciones del aviso de error
    override fun cancelarAvisoDeError() {
        cantidadCancelacionesDeAvisoDeError++
    }
}
