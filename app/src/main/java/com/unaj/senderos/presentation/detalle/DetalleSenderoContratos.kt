package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.Sendero

enum class SeccionDetalle { PUNTOS_DE_INTERES, RECOMENDACIONES, SERVICIOS, ETAPAS }

data class DetalleSenderoEstadoVisual(
    val sendero: Sendero? = null,
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val seccionesExpandidas: Set<SeccionDetalle> = emptySet(),
    val esMenuOpcionesVisible: Boolean = false,
    val esDialogoEliminarVisible: Boolean = false,
    val hayRecorridoEnCursoEnEsteSendero: Boolean = false,
    val esRecorridoEnCursoSimulado: Boolean = false,
    val nombreSenderoEnCursoAReemplazar: String? = null,
    val esModoDemostracion: Boolean = false,
    val puntoEnfocado: Coordenada? = null,
    val contadorEnfoque: Int = 0,
    val puntosDeInteres: List<PuntoDeInteres> = emptyList(),
    val estadoMapaSinConexion: EstadoMapaSinConexion = EstadoMapaSinConexion.NoDescargado,
    val esDialogoEliminarMapaVisible: Boolean = false,
    val esSugerenciaDescargaVisible: Boolean = false
)

sealed interface DetalleSenderoEvento {
    data object AlVolver : DetalleSenderoEvento
    data object AlAlternarFavorito : DetalleSenderoEvento
    data object AlCompartir : DetalleSenderoEvento
    data class AlAlternarSeccion(val seccion: SeccionDetalle) : DetalleSenderoEvento
    data class AlSeleccionarPuntoDeInteres(val indice: Int) : DetalleSenderoEvento
    data object AlTocarIniciarRecorrido : DetalleSenderoEvento
    data object AlTocarDescargarMapa : DetalleSenderoEvento
    data object AlTocarEliminarMapa : DetalleSenderoEvento
    data object AlConfirmarEliminarMapa : DetalleSenderoEvento
    data object AlCancelarEliminarMapa : DetalleSenderoEvento
    data object AlDescargarEIniciar : DetalleSenderoEvento
    data object AlIniciarSinDescargar : DetalleSenderoEvento
    data object AlConcederPermisoUbicacion : DetalleSenderoEvento
    data object AlDenegarPermisoUbicacion : DetalleSenderoEvento
    data object AlConcederSoloUbicacionAproximada : DetalleSenderoEvento
    data object AlConfirmarReemplazarRecorrido : DetalleSenderoEvento
    data object AlCancelarReemplazarRecorrido : DetalleSenderoEvento
    data object AlAbrirMenuOpciones : DetalleSenderoEvento
    data object AlCerrarMenuOpciones : DetalleSenderoEvento
    data object AlTocarEditar : DetalleSenderoEvento
    data object AlTocarEliminar : DetalleSenderoEvento
    data object AlCancelarEliminar : DetalleSenderoEvento
    data object AlConfirmarEliminar : DetalleSenderoEvento
}

sealed interface DetalleSenderoEfecto {
    data object VolverAlListado : DetalleSenderoEfecto
    data class MostrarAviso(val mensaje: String) : DetalleSenderoEfecto
    data class CompartirSendero(val texto: String) : DetalleSenderoEfecto
    data class NavegarAEdicion(val idSendero: String) : DetalleSenderoEfecto
    data object SolicitarPermisoUbicacion : DetalleSenderoEfecto
    data class NavegarARecorrido(val idSendero: String) : DetalleSenderoEfecto
}
