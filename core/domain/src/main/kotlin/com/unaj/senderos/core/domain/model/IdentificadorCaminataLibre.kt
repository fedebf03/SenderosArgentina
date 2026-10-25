package com.unaj.senderos.core.domain.model

private const val PREFIJO_CAMINATA_LIBRE = "libre-"
private const val PREFIJO_SENDERO_DESDE_CAMINATA = "desde-"

object IdentificadorCaminataLibre {

    // Hace de idSendero en el recorrido realizado y en los puntos capturados
    fun crear(instanteInicioMilisegundos: Long): String = "$PREFIJO_CAMINATA_LIBRE$instanteInicioMilisegundos"

    fun esDeCaminataLibre(idSendero: String): Boolean = idSendero.startsWith(PREFIJO_CAMINATA_LIBRE)

    // Siempre da el mismo identificador. Así convertir la caminata dos veces reemplaza al sendero en lugar de duplicarlo
    // y la app puede saber si ya se convirtió
    fun idSenderoDesde(idCaminata: String): String = "$PREFIJO_SENDERO_DESDE_CAMINATA$idCaminata"
}
