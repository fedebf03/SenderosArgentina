package com.unaj.senderos.presentation.perfil

import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.domain.model.Usuario

// Funciones del equipo que la app informa al usuario
enum class FuncionDispositivo { GPS, ACELEROMETRO, BRUJULA, CAMARA }

// Disponibilidad de una función del equipo
data class EstadoFuncion(val funcion: FuncionDispositivo, val estaDisponible: Boolean)

data class PerfilEstadoVisual(
    val usuario: Usuario? = null,
    val esModoDemostracion: Boolean = false,
    val recorridos: List<RecorridoRealizado> = emptyList(),
    val funcionesDispositivo: List<EstadoFuncion> = emptyList(),
    val mensajeError: String? = null,
    val hayRecorridoEnCurso: Boolean = false,
    val esDialogoCerrarSesionVisible: Boolean = false
)

sealed interface PerfilEvento {
    data object AlTocarCerrarSesion : PerfilEvento
    data object AlConfirmarCerrarSesion : PerfilEvento
    data object AlCancelarCerrarSesion : PerfilEvento
    data class AlCambiarModoDemostracion(val activo: Boolean) : PerfilEvento
    data class AlSeleccionarRecorrido(val idRecorrido: String) : PerfilEvento
}

sealed interface PerfilEfecto {
    data object NavegarALogin : PerfilEfecto
    data class NavegarAHistorialDetalle(val idRecorrido: String) : PerfilEfecto
}
