package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.ui.sendero.PuntoDeInteres
import com.unaj.senderos.core.domain.usecase.FormatoExportacion
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.ui.revisionpuntos.RevisionPuntosEstadoVisual
import com.unaj.senderos.core.ui.revisionpuntos.RevisionPuntosEvento

enum class SeccionDetalle { PUNTOS_DE_INTERES, RECOMENDACIONES, SERVICIOS, ETAPAS }

data class DetalleSenderoEstadoVisual(
    val sendero: Sendero? = null,
    val estaCargando: Boolean = true,
    val mensajeError: String? = null,
    val seccionesExpandidas: Set<SeccionDetalle> = emptySet(),
    val esMenuOpcionesVisible: Boolean = false,
    val esDialogoEliminarVisible: Boolean = false,
    val esDialogoExportarVisible: Boolean = false,
    val hayRecorridoEnCursoEnEsteSendero: Boolean = false,
    val esRecorridoEnCursoSimulado: Boolean = false,
    val nombreSenderoEnCursoAReemplazar: String? = null,
    val esModoDemostracion: Boolean = false,
    val puntoEnfocado: Coordenada? = null,
    val indicePuntoSeleccionado: Int? = null,
    val contadorEnfoque: Int = 0,
    val puntosDeInteres: List<PuntoDeInteres> = emptyList(),
    val estadoMapaSinConexion: EstadoMapaSinConexion = EstadoMapaSinConexion.NoDescargado,
    val esDialogoEliminarMapaVisible: Boolean = false,
    val esSugerenciaDescargaVisible: Boolean = false,
    val esDialogoSinGpsVisible: Boolean = false,
    val revision: RevisionPuntosEstadoVisual = RevisionPuntosEstadoVisual(),
    val fotoAmpliada: String? = null,
    val estaPublicado: Boolean = false,
    val estaPublicando: Boolean = false,
    val esDialogoPublicarVisible: Boolean = false,
    val esDialogoHacerPrivadoVisible: Boolean = false,
    val estaCalculandoPerfil: Boolean = false,
    val perfilEstaDesactualizado: Boolean = false,
    val debeDesplazarAlMapa: Boolean = true
) {
    // Indicar si se ofrece revisar los puntos capturados; durante un recorrido en curso se espera a que termine
    val hayPuntosPorRevisar: Boolean
        get() = revision.hayPuntosPorRevisar && !hayRecorridoEnCursoEnEsteSendero
}

sealed interface DetalleSenderoEvento {
    data object AlTocarExportar : DetalleSenderoEvento
    data object AlCerrarExportar : DetalleSenderoEvento
    data class AlElegirFormatoExportacion(val formato: FormatoExportacion) : DetalleSenderoEvento
    data class AlExportarSendero(val formato: FormatoExportacion, val uri: String) : DetalleSenderoEvento
    data object AlVolver : DetalleSenderoEvento
    data object AlAlternarFavorito : DetalleSenderoEvento
    data object AlCompartir : DetalleSenderoEvento
    data class AlAlternarSeccion(val seccion: SeccionDetalle) : DetalleSenderoEvento
    data class AlSeleccionarPuntoDeInteres(val indice: Int) : DetalleSenderoEvento
    data class AlSeleccionarPuntoEnMapa(val indice: Int?) : DetalleSenderoEvento
    data class AlSeleccionarKilometroDelPerfil(val kilometros: Double) : DetalleSenderoEvento
    data object AlCalcularPerfil : DetalleSenderoEvento
    data object AlTocarIniciarRecorrido : DetalleSenderoEvento
    data object AlTocarDescargarMapa : DetalleSenderoEvento
    data object AlTocarEliminarMapa : DetalleSenderoEvento
    data object AlConfirmarEliminarMapa : DetalleSenderoEvento
    data object AlCancelarEliminarMapa : DetalleSenderoEvento
    data object AlDescargarEIniciar : DetalleSenderoEvento
    data object AlIniciarSinDescargar : DetalleSenderoEvento
    data object AlIniciarSinGps : DetalleSenderoEvento
    data object AlCancelarSinGps : DetalleSenderoEvento
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
    data object AlTocarPublicar : DetalleSenderoEvento
    data object AlConfirmarPublicar : DetalleSenderoEvento
    data object AlCancelarPublicar : DetalleSenderoEvento
    data object AlTocarHacerPrivado : DetalleSenderoEvento
    data object AlConfirmarHacerPrivado : DetalleSenderoEvento
    data object AlCancelarHacerPrivado : DetalleSenderoEvento
    data class AlRevisionPuntos(val evento: RevisionPuntosEvento) : DetalleSenderoEvento
    data class AlAmpliarFoto(val foto: String) : DetalleSenderoEvento
    data object AlCerrarFotoAmpliada : DetalleSenderoEvento
}

sealed interface DetalleSenderoEfecto {
    data object VolverAlListado : DetalleSenderoEfecto
    data class MostrarAviso(val mensaje: String) : DetalleSenderoEfecto
    data class CompartirSendero(val texto: String) : DetalleSenderoEfecto
    data class NavegarAEdicion(val idSendero: String) : DetalleSenderoEfecto
    data object SolicitarPermisoUbicacion : DetalleSenderoEfecto
    data class NavegarARecorrido(val idSendero: String) : DetalleSenderoEfecto
    // Abrir el selector del sistema para elegir dónde guardar el archivo exportado
    data class ElegirDestinoExportacion(val formato: FormatoExportacion, val nombreArchivo: String) : DetalleSenderoEfecto
}
