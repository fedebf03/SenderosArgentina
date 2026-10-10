package com.unaj.senderos.presentation.formulario

import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.TipoRecorrido

enum class CampoFormulario {
    NOMBRE,
    REGION,
    DESCRIPCION,
    DISTANCIA,
    DESNIVEL,
    ALTURA_MAXIMA,
    ALTURA_MINIMA,
    DURACION,
    PUNTO_INICIO,
    MEJOR_EPOCA,
    NUEVA_RECOMENDACION,
    NUEVO_SERVICIO,
    NOMBRE_NUEVA_ETAPA,
    DISTANCIA_NUEVA_ETAPA
}

data class FormularioSenderoEstadoVisual(
    val esEdicion: Boolean = false,
    val estaCargando: Boolean = false,
    val estaGuardando: Boolean = false,
    val nombre: String = "",
    val region: String = "",
    val descripcion: String = "",
    val distancia: String = "",
    val desnivel: String = "",
    val alturaMaxima: String = "",
    val alturaMinima: String = "",
    val duracionEstimada: String = "",
    val puntoInicio: String = "",
    val mejorEpoca: String = "",
    val dificultad: Dificultad = Dificultad.MEDIA,
    val tipoRecorrido: TipoRecorrido = TipoRecorrido.LINEAL,
    val recomendaciones: List<String> = emptyList(),
    val servicios: List<String> = emptyList(),
    val etapas: List<Etapa> = emptyList(),
    val textoNuevaRecomendacion: String = "",
    val textoNuevoServicio: String = "",
    val nombreNuevaEtapa: String = "",
    val distanciaNuevaEtapa: String = "",
    val camposConError: Set<CampoSendero> = emptySet(),
    val hayCambiosSinGuardar: Boolean = false,
    val esDialogoDescartarVisible: Boolean = false,
    val mensajeError: String? = null
)

sealed interface FormularioSenderoEvento {
    data class AlCambiarTexto(val campo: CampoFormulario, val texto: String) : FormularioSenderoEvento
    data class AlSeleccionarDificultad(val dificultad: Dificultad) : FormularioSenderoEvento
    data class AlSeleccionarTipoRecorrido(val tipoRecorrido: TipoRecorrido) : FormularioSenderoEvento
    data object AlAgregarRecomendacion : FormularioSenderoEvento
    data class AlQuitarRecomendacion(val indice: Int) : FormularioSenderoEvento
    data object AlAgregarServicio : FormularioSenderoEvento
    data class AlQuitarServicio(val indice: Int) : FormularioSenderoEvento
    data object AlAgregarEtapa : FormularioSenderoEvento
    data class AlQuitarEtapa(val indice: Int) : FormularioSenderoEvento
    data object AlGuardar : FormularioSenderoEvento
    data object AlVolver : FormularioSenderoEvento
    data object AlConfirmarDescartar : FormularioSenderoEvento
    data object AlCancelarDescartar : FormularioSenderoEvento
}

sealed interface FormularioSenderoEfecto {
    data object VolverAtras : FormularioSenderoEfecto
    data class SenderoGuardado(val idSendero: String) : FormularioSenderoEfecto
    data class MostrarAviso(val mensaje: String) : FormularioSenderoEfecto
}
