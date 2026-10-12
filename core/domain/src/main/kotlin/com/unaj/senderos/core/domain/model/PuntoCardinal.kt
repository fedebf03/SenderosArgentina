package com.unaj.senderos.domain.model

// Los ocho puntos cardinales y colaterales en español, en sentido horario desde el norte, con su símbolo corto
enum class PuntoCardinal(val simbolo: String) {
    NORTE("N"),
    NORESTE("NE"),
    ESTE("E"),
    SURESTE("SE"),
    SUR("S"),
    SUROESTE("SO"),
    OESTE("O"),
    NOROESTE("NO")
}
