package com.unaj.senderos.presentation.perfil

import com.unaj.senderos.domain.model.CapacidadesDispositivo

fun obtenerNombreFuncion(funcion: FuncionDispositivo): String {
    return when (funcion) {
        FuncionDispositivo.GPS -> "GPS"
        FuncionDispositivo.ACELEROMETRO -> "Acelerómetro (pasos)"
        FuncionDispositivo.BRUJULA -> "Brújula"
        FuncionDispositivo.CAMARA -> "Cámara"
    }
}

fun explicarFuncionNoDisponible(funcion: FuncionDispositivo): String {
    return when (funcion) {
        FuncionDispositivo.GPS -> "Sin GPS, la ubicación sale del wifi o de la red móvil: es menos precisa y no funciona sin señal"
        FuncionDispositivo.ACELEROMETRO -> "Sin acelerómetro no se cuentan los pasos ni el ritmo"
        FuncionDispositivo.BRUJULA -> "Sin brújula, la dirección se calcula solo mientras caminás"
        FuncionDispositivo.CAMARA -> "Sin cámara no se pueden sacar fotos; sí elegirlas de la galería"
    }
}

fun listarFuncionesDispositivo(capacidades: CapacidadesDispositivo): List<EstadoFuncion> {
    return listOf(
        EstadoFuncion(FuncionDispositivo.GPS, capacidades.tieneGps),
        EstadoFuncion(FuncionDispositivo.ACELEROMETRO, capacidades.tieneAcelerometro),
        EstadoFuncion(FuncionDispositivo.BRUJULA, capacidades.tieneBrujula),
        EstadoFuncion(FuncionDispositivo.CAMARA, capacidades.tieneCamara)
    )
}
