package com.unaj.senderos.core.domain.notificaciones

// progresoPorcentaje nulo significa progreso indeterminado
data class ContenidoNotificacionRecorrido(
    val titulo: String,
    val textoCorto: String,
    val textoExpandido: String,
    val progresoPorcentaje: Int?,
    // Instante de inicio para el cronómetro del sistema; nulo si se desconoce
    val instanteInicioMilisegundos: Long? = null,
    // Falso en una caminata libre, que no tiene un largo contra el cual medir el avance: no se dibuja ninguna barra
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
