package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.domain.CalculadoraRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.model.filtrarSenderosPorTexto

data class SenderosEstadoVisual(
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val textoBusqueda: String = "",
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val ubicacionActual: Coordenada? = null,
    val ordenarPorCercania: Boolean = false,
    val seSolicitoPermisoNotificaciones: Boolean = false,
    val estaSinConexion: Boolean = false
) {
    // Calcular la distancia en kilómetros al primer punto del recorrido; null sin recorrido o sin ubicación
    fun distanciaKilometrosA(sendero: Sendero): Double? {
        val ubicacion = ubicacionActual ?: return null
        val primerPunto = sendero.recorrido.firstOrNull() ?: return null
        return CalculadoraRecorrido.distanciaEntre(ubicacion, primerPunto)
    }

    // Senderos filtrados por búsqueda y, si corresponde, ordenados por cercanía; se calcula una sola vez por estado
    val senderosFiltrados: List<Sendero> = calcularSenderosFiltrados()

    // Calcular la lista filtrada y ordenada dejando al final los senderos sin distancia
    private fun calcularSenderosFiltrados(): List<Sendero> {
        val filtrados = filtrarSenderosPorTexto(senderos, textoBusqueda)
        if (!ordenarPorCercania) return filtrados
        return filtrados
            .map { sendero -> sendero to (distanciaKilometrosA(sendero) ?: Double.MAX_VALUE) }
            .sortedBy { (_, distancia) -> distancia }
            .map { (sendero, _) -> sendero }
    }
}

sealed interface SenderosEvento {
    data class AlCambiarBusqueda(val texto: String) : SenderosEvento
    data class AlCambiarOrdenCercania(val activado: Boolean) : SenderosEvento
    data object AlObtenerUbicacion : SenderosEvento
    data object AlSolicitarPermisoNotificaciones : SenderosEvento
    data object AlTocarCrearSendero : SenderosEvento
    data class AlSeleccionarSendero(val idSendero: String) : SenderosEvento
}

sealed interface SenderosEfecto {
    data class NavegarADetalle(val idSendero: String) : SenderosEfecto
    data object NavegarACreacion : SenderosEfecto
    data class MostrarAviso(val mensaje: String) : SenderosEfecto
}
