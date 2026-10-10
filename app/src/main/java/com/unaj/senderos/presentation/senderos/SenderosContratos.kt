package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario

enum class SeccionNavegacion { INICIO, MAPA, NAVEGAR, FAVORITOS, PERFIL }

data class SenderosEstadoVisual(
    val usuario: Usuario? = null,
    val senderos: List<Sendero> = emptyList(),
    val textoBusqueda: String = "",
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val seccionSeleccionada: SeccionNavegacion = SeccionNavegacion.INICIO,
    val esMenuPerfilVisible: Boolean = false
) {
    // Filtrar los senderos cuyo nombre o región contienen el texto de búsqueda
    val senderosFiltrados: List<Sendero>
        get() {
            val textoNormalizado = textoBusqueda.trim()
            if (textoNormalizado.isEmpty()) return senderos
            return senderos.filter { sendero ->
                sendero.nombre.contains(textoNormalizado, ignoreCase = true) ||
                    sendero.region.contains(textoNormalizado, ignoreCase = true)
            }
        }
}

sealed interface SenderosEvento {
    data class AlCambiarBusqueda(val texto: String) : SenderosEvento
    data class AlSeleccionarSeccion(val seccion: SeccionNavegacion) : SenderosEvento
    data object AlTocarAvatar : SenderosEvento
    data object AlCerrarMenuPerfil : SenderosEvento
    data object AlCerrarSesion : SenderosEvento
    data object AlTocarNotificaciones : SenderosEvento
    data object AlTocarCrearSendero : SenderosEvento
    data class AlSeleccionarSendero(val idSendero: String) : SenderosEvento
}

sealed interface SenderosEfecto {
    data object NavegarALogin : SenderosEfecto
    data class NavegarADetalle(val idSendero: String) : SenderosEfecto
    data object NavegarACreacion : SenderosEfecto
    data class MostrarAviso(val mensaje: String) : SenderosEfecto
}
