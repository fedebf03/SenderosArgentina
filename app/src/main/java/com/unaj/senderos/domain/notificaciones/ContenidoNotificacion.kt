package com.unaj.senderos.domain.notificaciones

// Contenido de la notificación persistente del recorrido; progresoPorcentaje nulo significa progreso indeterminado
data class ContenidoNotificacionRecorrido(
    val titulo: String,
    val textoCorto: String,
    val textoExpandido: String,
    val progresoPorcentaje: Int?,
    // Instante de inicio para el cronómetro del sistema; nulo si se desconoce
    val instanteInicioMilisegundos: Long? = null
)

// Contenido de un aviso del recorrido (cercanía a un punto, desvío o llegada)
data class ContenidoAvisoRecorrido(
    val titulo: String,
    val texto: String,
    val tipoVisual: TipoAvisoVisual,
    val permiteFinalizar: Boolean = false
)

// Contenido de una notificación de sincronización; permiteReintentar agrega la acción "Reintentar"
data class ContenidoNotificacionSincronizacion(
    val titulo: String,
    val texto: String,
    val permiteReintentar: Boolean
)
