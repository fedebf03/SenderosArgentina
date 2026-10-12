package com.unaj.senderos.domain.model

// Resultado de procesar una nueva posición: el estado actualizado y los avisos nuevos
data class ResultadoSeguimiento(
    val estado: EstadoSeguimiento,
    val avisos: List<AvisoRecorrido>
)
