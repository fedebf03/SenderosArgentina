package com.unaj.senderos.falsos

import com.unaj.senderos.domain.port.PreferenciasAplicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FalsoPreferenciasAplicacion(modoInicial: Boolean = false) : PreferenciasAplicacion {

    private val modo = MutableStateFlow(modoInicial)

    override val esModoDemostracion: StateFlow<Boolean> = modo

    override fun cambiarModoDemostracion(activo: Boolean) {
        modo.value = activo
    }
}
