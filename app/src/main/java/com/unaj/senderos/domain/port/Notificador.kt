package com.unaj.senderos.domain.port

import com.unaj.senderos.domain.notificaciones.MensajeNotificacion

interface Notificador {

    fun notificar(mensaje: MensajeNotificacion)

    fun cancelarAvisoDeError()
}
