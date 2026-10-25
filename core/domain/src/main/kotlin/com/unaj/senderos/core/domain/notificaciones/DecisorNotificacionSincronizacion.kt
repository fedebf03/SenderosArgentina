package com.unaj.senderos.core.domain.notificaciones

import com.unaj.senderos.core.domain.model.ResumenSincronizacion

object DecisorNotificacionSincronizacion {

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
