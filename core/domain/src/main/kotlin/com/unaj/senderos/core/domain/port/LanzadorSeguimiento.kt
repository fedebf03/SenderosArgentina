package com.unaj.senderos.domain.repository

interface LanzadorSeguimiento {

    // Lanzar el seguimiento en segundo plano del sendero indicado, real o simulado
    fun iniciar(idSendero: String, esSimulado: Boolean = false)

    // Detener el seguimiento en segundo plano
    fun detener()
}
