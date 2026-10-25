package com.unaj.senderos.core.domain.model

sealed interface EstadoMapaSinConexion {
    data object NoDescargado : EstadoMapaSinConexion

    data class Descargando(val porcentaje: Int, val bytesCompletados: Long) : EstadoMapaSinConexion

    // La descarga sigue activa aunque se perdió la conexión o el servidor no responde. Se reanuda sola
    data class EsperandoConexion(val porcentaje: Int, val bytesCompletados: Long) : EstadoMapaSinConexion

    data class Disponible(val bytes: Long) : EstadoMapaSinConexion

    // El mapa descargado corresponde a un trazado anterior del sendero
    data class Desactualizado(val bytes: Long) : EstadoMapaSinConexion

    data class ConError(val mensaje: String) : EstadoMapaSinConexion
}
