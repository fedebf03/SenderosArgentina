package com.unaj.senderos.core.domain.notificaciones

// Un progresoPorcentaje nulo significa progreso indeterminado
data class ContenidoNotificacionRecorrido(
    val titulo: String,
    val textoCorto: String,
    val textoExpandido: String,
    val progresoPorcentaje: Int?,
    // Lo usa el cronómetro del sistema en la notificación
    val instanteInicioMilisegundos: Long? = null,
    // Una caminata libre no tiene un largo contra el cual medir el avance, por eso no se dibuja la barra
    val tieneProgreso: Boolean = true
)

data class ContenidoAvisoRecorrido(
    val titulo: String,
    val texto: String,
    val tipoVisual: TipoAvisoVisual,
    val permiteFinalizar: Boolean = false
)

data class ContenidoNotificacionSincronizacion(
    val titulo: String,
    val texto: String,
    val permiteReintentar: Boolean
)
