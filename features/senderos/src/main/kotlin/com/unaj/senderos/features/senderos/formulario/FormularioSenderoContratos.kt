package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido

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

// Qué hace un toque en el mapa del editor: agregar un punto al final del camino o marcar un punto de interés
enum class ModoEditorRecorrido { TRAZAR_CAMINO, MARCAR_PUNTO_DE_INTERES }

// Punto de interés que se está describiendo en el menú, antes de insertarlo en el trazado; foto es la ruta de la copia local
data class PuntoDeInteresNuevo(
    val latitud: Double,
    val longitud: Double,
    val tipo: TipoPunto = TipoPunto.MIRADOR,
    val nombre: String = "",
    val foto: String = "",
    val estaProcesandoFoto: Boolean = false,
    val mensajeError: String? = null
) {
    val hayDatosCargados: Boolean
        get() = nombre.isNotBlank() || foto.isNotEmpty() || estaProcesandoFoto
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
    val bioma: Bioma = Bioma.SIN_DEFINIR,
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
    val mensajeError: String? = null,
    val recorrido: List<PuntoRecorrido> = emptyList(),
    val esEditorMapaVisible: Boolean = false,
    val indicePuntoSeleccionado: Int? = null,
    val modoEditor: ModoEditorRecorrido = ModoEditorRecorrido.TRAZAR_CAMINO,
    // No nulo mientras el menú del punto de interés está abierto
    val puntoDeInteresNuevo: PuntoDeInteresNuevo? = null,
    // Índice del punto ya trazado cuya foto se está copiando; null si no hay ninguna copia en curso
    val indicePuntoCopiandoFoto: Int? = null,
    val foto: String = "",
    val estaProcesandoFoto: Boolean = false
) {
    val distanciaCalculadaKilometros: Double
        get() = CalculadoraRecorrido.distanciaKilometros(recorrido, tipoRecorrido)
}

sealed interface FormularioSenderoEvento {
    data class AlCambiarTexto(val campo: CampoFormulario, val texto: String) : FormularioSenderoEvento
    data class AlSeleccionarDificultad(val dificultad: Dificultad) : FormularioSenderoEvento
    data class AlSeleccionarBioma(val bioma: Bioma) : FormularioSenderoEvento
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
    data object AlAbrirEditorMapa : FormularioSenderoEvento
    data object AlCerrarEditorMapa : FormularioSenderoEvento
    data class AlAgregarPunto(val latitud: Double, val longitud: Double) : FormularioSenderoEvento
    data class AlMoverPunto(val indice: Int, val latitud: Double, val longitud: Double) : FormularioSenderoEvento
    data class AlSeleccionarPunto(val indice: Int?) : FormularioSenderoEvento
    data class AlEliminarPunto(val indice: Int) : FormularioSenderoEvento
    data class AlCambiarTipoPunto(val indice: Int, val tipo: TipoPunto) : FormularioSenderoEvento
    data class AlCambiarNombrePunto(val indice: Int, val nombre: String) : FormularioSenderoEvento
    data object AlInvertirSentido : FormularioSenderoEvento
    data class AlInsertarPuntoAntes(val indice: Int) : FormularioSenderoEvento
    data class AlCambiarModoEditor(val modo: ModoEditorRecorrido) : FormularioSenderoEvento
    data class AlElegirTipoPuntoNuevo(val tipo: TipoPunto) : FormularioSenderoEvento
    data class AlCambiarNombrePuntoNuevo(val nombre: String) : FormularioSenderoEvento
    data class AlSeleccionarFotoPuntoNuevo(val uri: String) : FormularioSenderoEvento
    data object AlQuitarFotoPuntoNuevo : FormularioSenderoEvento
    data class AlFallarFotoPuntoNuevo(val mensaje: String) : FormularioSenderoEvento
    data object AlCancelarPuntoNuevo : FormularioSenderoEvento
    data class AlSeleccionarFotoDePunto(val indice: Int, val uri: String) : FormularioSenderoEvento
    data class AlQuitarFotoDePunto(val indice: Int) : FormularioSenderoEvento
    data class AlFallarFotoDePunto(val mensaje: String) : FormularioSenderoEvento
    data object AlGuardarPuntoNuevo : FormularioSenderoEvento
    data class AlSeleccionarFoto(val uri: String) : FormularioSenderoEvento
    data object AlQuitarFoto : FormularioSenderoEvento
}

sealed interface FormularioSenderoEfecto {
    data object VolverAtras : FormularioSenderoEfecto
    data class SenderoGuardado(val idSendero: String) : FormularioSenderoEfecto
    data class MostrarAviso(val mensaje: String) : FormularioSenderoEfecto
}
