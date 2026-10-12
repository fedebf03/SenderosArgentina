package com.unaj.senderos.domain.model

// Milisegundos de demora desde la modificación a partir de los cuales un sendero se considera guardado sin conexión
const val UMBRAL_DEMORA_MILISEGUNDOS = 60_000L

// Resultado de una sincronización: cuántos senderos se subieron, cuántos esperaron sin conexión y cuántos fallaron
data class ResumenSincronizacion(
    val senderosSubidos: Int,
    val senderosGuardadosSinConexion: Int,
    val senderosConError: Int,
    val primerError: Throwable?
)
