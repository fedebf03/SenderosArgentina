package com.unaj.senderos.presentation.perfil

import com.unaj.senderos.domain.model.CapacidadesDispositivo

// Obtener el nombre visible de una función del equipo
fun obtenerNombreFuncion(funcion: FuncionDispositivo): String {
    return when (funcion) {
        FuncionDispositivo.GPS -> "GPS"
        FuncionDispositivo.ACELEROMETRO -> "Acelerómetro (pasos)"
        FuncionDispositivo.BRUJULA -> "Brújula"
        FuncionDispositivo.CAMARA -> "Cámara"
    }
}

// Explicar brevemente qué se pierde cuando el equipo no tiene la función
fun explicarFuncionNoDisponible(funcion: FuncionDispositivo): String {
    return when (funcion) {
        FuncionDispositivo.GPS -> "Sin GPS, la ubicación sale del wifi o de la red móvil: es menos precisa y no funciona sin señal"
        FuncionDispositivo.ACELEROMETRO -> "Sin acelerómetro no se cuentan los pasos ni el ritmo"
        FuncionDispositivo.BRUJULA -> "Sin brújula, la dirección se calcula solo mientras caminás"
        FuncionDispositivo.CAMARA -> "Sin cámara no se pueden sacar fotos; sí elegirlas de la galería"
    }
}

// Convertir las capacidades detectadas en la lista de funciones que se muestra en el perfil
fun listarFuncionesDispositivo(capacidades: CapacidadesDispositivo): List<EstadoFuncion> {
    return listOf(
        EstadoFuncion(FuncionDispositivo.GPS, capacidades.tieneGps),
        EstadoFuncion(FuncionDispositivo.ACELEROMETRO, capacidades.tieneAcelerometro),
        EstadoFuncion(FuncionDispositivo.BRUJULA, capacidades.tieneBrujula),
        EstadoFuncion(FuncionDispositivo.CAMARA, capacidades.tieneCamara)
    )
}
