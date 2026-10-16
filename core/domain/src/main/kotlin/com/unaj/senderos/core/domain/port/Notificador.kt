package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.notificaciones.MensajeNotificacion

interface Notificador {

    fun notificar(mensaje: MensajeNotificacion)

    fun cancelarAvisoDeError()
}
