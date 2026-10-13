package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.model.filtrarSenderosPorDificultad
import com.unaj.senderos.domain.model.filtrarSenderosPorTexto
import com.unaj.senderos.domain.recorrido.CalculadoraRecorrido

data class SenderosEstadoVisual(
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val textoBusqueda: String = "",
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val ubicacionActual: Coordenada? = null,
    val ordenarPorCercania: Boolean = false,
    val dificultadesSeleccionadas: Set<Dificultad> = emptySet(),
    val seSolicitoPermisoNotificaciones: Boolean = false,
    val estaSinConexion: Boolean = false
) {
    // Devuelve null sin recorrido o sin ubicación
    fun distanciaKilometrosA(sendero: Sendero): Double? {
        val ubicacion = ubicacionActual ?: return null
        val primerPunto = sendero.recorrido.firstOrNull() ?: return null
        return CalculadoraRecorrido.distanciaEntre(ubicacion, primerPunto)
    }

    // Se calcula una sola vez por estado
    val senderosFiltrados: List<Sendero> = calcularSenderosFiltrados()

    private fun calcularSenderosFiltrados(): List<Sendero> {
        val filtrados = filtrarSenderosPorDificultad(
            filtrarSenderosPorTexto(senderos, textoBusqueda),
            dificultadesSeleccionadas
        )
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
    data class AlAlternarDificultad(val dificultad: Dificultad) : SenderosEvento
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
