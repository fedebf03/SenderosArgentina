package com.unaj.senderos.domain.model

// Situación del mapa descargado de un sendero para usarlo sin señal
sealed interface EstadoMapaSinConexion {
    data object NoDescargado : EstadoMapaSinConexion

    data class Descargando(val porcentaje: Int, val bytesCompletados: Long) : EstadoMapaSinConexion

    // La descarga sigue activa pero se perdió la conexión o el servidor no responde; se reanuda sola
    data class EsperandoConexion(val porcentaje: Int, val bytesCompletados: Long) : EstadoMapaSinConexion

    data class Disponible(val bytes: Long) : EstadoMapaSinConexion

    // El mapa descargado corresponde a un trazado anterior del sendero
    data class Desactualizado(val bytes: Long) : EstadoMapaSinConexion

    data class ConError(val mensaje: String) : EstadoMapaSinConexion
}
