package com.unaj.senderos.core.domain.port

import kotlinx.coroutines.flow.StateFlow

interface PreferenciasAplicacion {

    val esModoDemostracion: StateFlow<Boolean>

    val esModoOscuro: StateFlow<Boolean>

    fun cambiarModoDemostracion(activo: Boolean)

    fun cambiarModoOscuro(activo: Boolean)
}
