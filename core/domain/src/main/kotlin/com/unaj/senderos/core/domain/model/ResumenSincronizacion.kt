package com.unaj.senderos.core.domain.model

// Con más de un minuto de demora desde la modificación se considera que el sendero se guardó sin conexión
const val UMBRAL_DEMORA_MILISEGUNDOS = 60_000L

data class ResumenSincronizacion(
    val senderosSubidos: Int,
    val senderosGuardadosSinConexion: Int,
    val senderosConError: Int,
    val primerError: Throwable?
)
