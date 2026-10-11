package com.unaj.senderos.presentation.mapasenderos

import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario

data class MapaSenderosEstadoVisual(
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val idSeleccionado: String? = null,
    val seSolicitoUbicacion: Boolean = false
) {
    val senderosEnMapa: List<Sendero>
        get() = senderos.filter { sendero -> sendero.recorrido.isNotEmpty() }

    val senderoSeleccionado: Sendero?
        get() = senderosEnMapa.firstOrNull { sendero -> sendero.id == idSeleccionado }
}

sealed interface MapaSenderosEvento {
    data class AlSeleccionarSendero(val idSendero: String?) : MapaSenderosEvento
    data class AlAbrirDetalle(val idSendero: String) : MapaSenderosEvento
    data object AlSolicitarUbicacion : MapaSenderosEvento
}

sealed interface MapaSenderosEfecto {
    data class NavegarADetalle(val idSendero: String) : MapaSenderosEfecto
}
