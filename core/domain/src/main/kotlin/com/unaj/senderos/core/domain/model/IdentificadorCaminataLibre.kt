package com.unaj.senderos.core.domain.model

private const val PREFIJO_CAMINATA_LIBRE = "libre-"

object IdentificadorCaminataLibre {

    // Crear el identificador de una caminata libre a partir del instante de inicio; hace de idSendero en el recorrido realizado y en los puntos capturados
    fun crear(instanteInicioMilisegundos: Long): String = "$PREFIJO_CAMINATA_LIBRE$instanteInicioMilisegundos"

    // Indicar si un idSendero corresponde a una caminata libre y no a un sendero guardado
    fun esDeCaminataLibre(idSendero: String): Boolean = idSendero.startsWith(PREFIJO_CAMINATA_LIBRE)
}
