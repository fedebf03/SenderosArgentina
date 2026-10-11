package com.unaj.senderos.presentation.favoritos

import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.model.filtrarSenderosPorTexto

data class FavoritosEstadoVisual(
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val textoBusqueda: String = "",
    val estaCargando: Boolean = true,
    val mensajeError: String? = null
) {
    val favoritos: List<Sendero>
        get() = senderos.filter { sendero -> sendero.esFavorito }

    val favoritosFiltrados: List<Sendero>
        get() = filtrarSenderosPorTexto(favoritos, textoBusqueda)
}

sealed interface FavoritosEvento {
    data class AlCambiarBusqueda(val texto: String) : FavoritosEvento
    data class AlSeleccionarSendero(val idSendero: String) : FavoritosEvento
}

sealed interface FavoritosEfecto {
    data class NavegarADetalle(val idSendero: String) : FavoritosEfecto
}
