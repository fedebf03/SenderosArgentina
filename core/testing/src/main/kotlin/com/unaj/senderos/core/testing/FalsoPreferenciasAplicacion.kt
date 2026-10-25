package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.PreferenciasAplicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FalsoPreferenciasAplicacion(
    modoInicial: Boolean = false,
    modoOscuroInicial: Boolean = false
) : PreferenciasAplicacion {

    private val modo = MutableStateFlow(modoInicial)
    private val modoOscuro = MutableStateFlow(modoOscuroInicial)

    override val esModoDemostracion: StateFlow<Boolean> = modo

    override val esModoOscuro: StateFlow<Boolean> = modoOscuro

    override fun cambiarModoDemostracion(activo: Boolean) {
        modo.value = activo
    }

    override fun cambiarModoOscuro(activo: Boolean) {
        modoOscuro.value = activo
    }
}
