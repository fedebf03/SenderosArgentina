package com.unaj.senderos.domain.notificaciones

import com.unaj.senderos.domain.model.ResumenSincronizacion

object DecisorNotificacionSincronizacion {

    // Decidir qué notificación mostrar según el resumen de la sincronización; null si no corresponde ninguna
    fun decidir(resumen: ResumenSincronizacion, esUltimoIntento: Boolean): MensajeNotificacion? {
        if (esUltimoIntento && resumen.senderosConError > 0) {
            return MensajeNotificacion.SenderosConError(resumen.senderosConError)
        }
        if (resumen.senderosGuardadosSinConexion > 0) {
            return MensajeNotificacion.SenderosSubidos(resumen.senderosGuardadosSinConexion)
        }
        return null
    }
}
