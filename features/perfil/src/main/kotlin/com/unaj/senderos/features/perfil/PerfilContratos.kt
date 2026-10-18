package com.unaj.senderos.features.perfil

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.AnalisisCopiaSeguridad
import com.unaj.senderos.core.domain.usecase.ModoRestauracion

data class PerfilEstadoVisual(
    val usuario: Usuario? = null,
    val esModoDemostracion: Boolean = false,
    val recorridos: List<RecorridoRealizado> = emptyList(),
    val mensajeError: String? = null,
    val hayRecorridoEnCurso: Boolean = false,
    val esDialogoCerrarSesionVisible: Boolean = false,
    val estaProcesandoCopia: Boolean = false,
    // Copia leída que tiene senderos repetidos y espera que el usuario elija qué hacer con ellos
    val copiaPendiente: AnalisisCopiaSeguridad? = null
)

sealed interface PerfilEvento {
    data object AlTocarCerrarSesion : PerfilEvento
    data object AlConfirmarCerrarSesion : PerfilEvento
    data object AlCancelarCerrarSesion : PerfilEvento
    data class AlCambiarModoDemostracion(val activo: Boolean) : PerfilEvento
    data class AlSeleccionarRecorrido(val idRecorrido: String) : PerfilEvento
    data class AlExportarCopiaSeguridad(val uri: String) : PerfilEvento
    data class AlImportarCopiaSeguridad(val uri: String) : PerfilEvento
    data class AlElegirModoRestauracion(val modo: ModoRestauracion) : PerfilEvento
    data object AlCancelarRestauracion : PerfilEvento
}

sealed interface PerfilEfecto {
    data object NavegarALogin : PerfilEfecto
    data class NavegarAHistorialDetalle(val idRecorrido: String) : PerfilEfecto
    data class MostrarAviso(val mensaje: String) : PerfilEfecto
}
