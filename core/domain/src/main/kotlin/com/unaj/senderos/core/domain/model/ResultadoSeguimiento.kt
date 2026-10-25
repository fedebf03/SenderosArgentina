package com.unaj.senderos.core.domain.model

data class ResultadoSeguimiento(
    val estado: EstadoSeguimiento,
    val avisos: List<AvisoRecorrido>
)
