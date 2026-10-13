package com.unaj.senderos.domain.model

data class ResultadoSeguimiento(
    val estado: EstadoSeguimiento,
    val avisos: List<AvisoRecorrido>
)
