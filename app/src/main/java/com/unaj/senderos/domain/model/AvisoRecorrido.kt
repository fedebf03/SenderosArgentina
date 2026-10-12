package com.unaj.senderos.domain.model

// Aviso que el seguimiento le comunica al usuario durante el recorrido
sealed interface AvisoRecorrido {
    data class CercaDePunto(val indice: Int, val punto: PuntoRecorrido) : AvisoRecorrido
    data object Desvio : AvisoRecorrido
}
