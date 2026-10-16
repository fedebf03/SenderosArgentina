package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.PreferenciasAplicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FalsoPreferenciasAplicacion(modoInicial: Boolean = false) : PreferenciasAplicacion {

    private val modo = MutableStateFlow(modoInicial)

    override val esModoDemostracion: StateFlow<Boolean> = modo

    override fun cambiarModoDemostracion(activo: Boolean) {
        modo.value = activo
    }
}
