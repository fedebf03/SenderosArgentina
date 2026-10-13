package com.unaj.senderos.domain.model

enum class TipoPunto { PASO, MIRADOR, AGUA, REFUGIO, CAMPING, PELIGRO }

// Punto georreferenciado del recorrido; el orden de la lista de puntos define el sentido
data class PuntoRecorrido(
    val latitud: Double,
    val longitud: Double,
    val tipo: TipoPunto = TipoPunto.PASO,
    val nombre: String = "",
    // Ruta local de la foto del punto o URL remota una vez sincronizada; vacío si no tiene
    val foto: String = ""
)
