package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.MensajeNotificacion

interface Notificador {

    // Mostrar al usuario el mensaje de notificación indicado
    fun notificar(mensaje: MensajeNotificacion)

    // Cancelar el aviso de error de sincronización si sigue visible
    fun cancelarAvisoDeError()
}
