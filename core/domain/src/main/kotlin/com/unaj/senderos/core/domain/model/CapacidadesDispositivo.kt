package com.unaj.senderos.core.domain.model

// La brújula equivale al sensor de vector de rotación, que es el que usa la app
data class CapacidadesDispositivo(
    val tieneGps: Boolean,
    val tieneAcelerometro: Boolean,
    val tieneBrujula: Boolean
) {
    companion object {
        val SIN_FUNCIONES = CapacidadesDispositivo(
            tieneGps = false,
            tieneAcelerometro = false,
            tieneBrujula = false
        )
    }
}
