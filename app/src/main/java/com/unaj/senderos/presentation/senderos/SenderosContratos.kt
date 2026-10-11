package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.model.filtrarSenderosPorTexto

data class SenderosEstadoVisual(
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val textoBusqueda: String = "",
    val estaCargando: Boolean = true,
    val mensajeError: String? = null
) {
    val senderosFiltrados: List<Sendero>
        get() = filtrarSenderosPorTexto(senderos, textoBusqueda)
}

sealed interface SenderosEvento {
    data class AlCambiarBusqueda(val texto: String) : SenderosEvento
    data object AlTocarCrearSendero : SenderosEvento
    data class AlSeleccionarSendero(val idSendero: String) : SenderosEvento
}

sealed interface SenderosEfecto {
    data class NavegarADetalle(val idSendero: String) : SenderosEfecto
    data object NavegarACreacion : SenderosEfecto
}
