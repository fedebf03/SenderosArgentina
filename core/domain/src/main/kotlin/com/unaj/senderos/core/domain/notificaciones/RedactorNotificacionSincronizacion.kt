package com.unaj.senderos.core.domain.notificaciones

object RedactorNotificacionSincronizacion {

    fun redactar(mensaje: MensajeNotificacion): ContenidoNotificacionSincronizacion {
        return when (mensaje) {
            is MensajeNotificacion.SenderosSubidos -> ContenidoNotificacionSincronizacion(
                titulo = "Senderos sincronizados",
                texto = if (mensaje.cantidad == 1) {
                    "Tu sendero guardado sin señal ya está en la nube"
                } else {
                    "Tus ${mensaje.cantidad} senderos guardados sin señal ya están en la nube"
                },
                permiteReintentar = false
            )
            is MensajeNotificacion.SenderosConError -> ContenidoNotificacionSincronizacion(
                titulo = "Error de sincronización",
                texto = if (mensaje.cantidad == 1) {
                    "No se pudo sincronizar 1 cambio (sendero o recorrido). Se reintentará al abrir la app"
                } else {
                    "No se pudieron sincronizar ${mensaje.cantidad} cambios (senderos o recorridos). Se reintentará al abrir la app"
                },
                permiteReintentar = true
            )
        }
    }
}
