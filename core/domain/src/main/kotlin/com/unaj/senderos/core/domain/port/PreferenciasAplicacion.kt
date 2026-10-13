package com.unaj.senderos.domain.port

import kotlinx.coroutines.flow.StateFlow

interface PreferenciasAplicacion {

    val esModoDemostracion: StateFlow<Boolean>

    fun cambiarModoDemostracion(activo: Boolean)
}
