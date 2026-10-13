package com.unaj.senderos.domain.port

interface LanzadorSeguimiento {

    // Lanzar el seguimiento en segundo plano del sendero indicado, real o simulado
    fun iniciar(idSendero: String, esSimulado: Boolean = false)

    // Detener el seguimiento en segundo plano guardando el recorrido en el historial
    fun detener()

    // Cancelar el seguimiento en segundo plano descartando lo recorrido, sin guardarlo en el historial
    fun cancelar()
}
