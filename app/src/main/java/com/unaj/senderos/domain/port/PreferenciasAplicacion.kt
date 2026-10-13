package com.unaj.senderos.domain.port

import kotlinx.coroutines.flow.StateFlow

interface PreferenciasAplicacion {

    // Indica si el modo demostración (recorrido simulado) está activo en este dispositivo
    val esModoDemostracion: StateFlow<Boolean>

    // Activar o desactivar el modo demostración y recordarlo en el dispositivo
    fun cambiarModoDemostracion(activo: Boolean)
}
