package com.unaj.senderos.domain.model

// Tipo de punto de interés dentro del recorrido de un sendero
enum class TipoPunto { PASO, MIRADOR, AGUA, REFUGIO, CAMPING, PELIGRO }

// Punto georreferenciado del recorrido; el orden de la lista de puntos define el sentido
data class PuntoRecorrido(
    val latitud: Double,
    val longitud: Double,
    val tipo: TipoPunto = TipoPunto.PASO,
    val nombre: String = ""
)
