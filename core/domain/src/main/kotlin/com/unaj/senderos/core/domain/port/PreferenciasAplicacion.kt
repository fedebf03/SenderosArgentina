package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.StateFlow

interface PreferenciasAplicacion {

    val esModoDemostracion: StateFlow<Boolean>

    fun cambiarModoDemostracion(activo: Boolean)
}
