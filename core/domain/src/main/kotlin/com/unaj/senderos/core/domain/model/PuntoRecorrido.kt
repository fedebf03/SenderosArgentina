package com.unaj.senderos.core.domain.model

enum class TipoPunto { PASO, MIRADOR, AGUA, REFUGIO, CAMPING, PELIGRO, OTRO }

// El orden de la lista de puntos define el sentido del recorrido
data class PuntoRecorrido(
    val latitud: Double,
    val longitud: Double,
    val tipo: TipoPunto = TipoPunto.PASO,
    val nombre: String = "",
    val foto: String = ""
)
