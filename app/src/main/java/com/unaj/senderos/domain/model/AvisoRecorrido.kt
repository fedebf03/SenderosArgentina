package com.unaj.senderos.domain.model

sealed interface AvisoRecorrido {
    data class CercaDePunto(val indice: Int, val punto: PuntoRecorrido) : AvisoRecorrido
    data object Desvio : AvisoRecorrido
    data object LlegadaAlFinal : AvisoRecorrido
}
