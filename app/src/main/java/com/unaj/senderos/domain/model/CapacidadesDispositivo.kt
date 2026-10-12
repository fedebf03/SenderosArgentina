package com.unaj.senderos.domain.model

// Funciones que el equipo puede ofrecer; la brújula equivale al sensor de vector de rotación, que es el que usa la app
data class CapacidadesDispositivo(
    val tieneGps: Boolean,
    val tieneAcelerometro: Boolean,
    val tieneBrujula: Boolean,
    val tieneCamara: Boolean
) {
    companion object {
        // Capacidades de un equipo sin ninguna función disponible, usadas como valor neutro
        val SIN_FUNCIONES = CapacidadesDispositivo(
            tieneGps = false,
            tieneAcelerometro = false,
            tieneBrujula = false,
            tieneCamara = false
        )
    }
}
