package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.recorrido.TrazadoImportado

enum class CampoFormulario {
    NOMBRE,
    REGION,
    DESCRIPCION,
    DISTANCIA,
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
    // Falso mientras la distancia sigue a la del mapa; verdadero cuando el usuario la escribió a mano
    val distanciaEsManual: Boolean = false,
    val perfilElevacion: PerfilElevacion? = null,
    val estaCalculandoPerfil: Boolean = false,
    // Motivo por el que no se pudo calcular el perfil, para mostrarlo con la opción de reintentar
    val avisoPerfil: String? = null,
    val estaImportandoTrazado: Boolean = false,
    // Trazado importado que espera confirmación porque reemplazaría el recorrido ya marcado
    val trazadoImportadoPendiente: TrazadoImportado? = null,
    val foto: String = "",
    val estaProcesandoFoto: Boolean = false,
    // Nombre del sendero que ya se creó desde esta misma caminata; mientras tenga valor se pregunta si se edita o se reemplaza
    val nombreSenderoYaConvertido: String? = null
) {
    val distanciaCalculadaKilometros: Double
        get() = CalculadoraRecorrido.distanciaKilometros(recorrido, tipoRecorrido)

    // Indicar si el perfil se calculó con otro trazado (se movió, agregó o borró un punto, o cambió el tipo de recorrido)
    val perfilEstaDesactualizado: Boolean
        get() = perfilElevacion != null && perfilElevacion.firmaTrazado != CalculadoraPerfil.firmaTrazado(recorrido, tipoRecorrido)
}

sealed interface FormularioSenderoEvento {
    data class AlCambiarTexto(val campo: CampoFormulario, val texto: String) : FormularioSenderoEvento
    data class AlSeleccionarDificultad(val dificultad: Dificultad) : FormularioSenderoEvento
    data class AlSeleccionarBioma(val bioma: Bioma) : FormularioSenderoEvento
    data class AlSeleccionarTipoRecorrido(val tipoRecorrido: TipoRecorrido) : FormularioSenderoEvento
    data object AlAgregarRecomendacion : FormularioSenderoEvento
    data class AlAgregarRecomendacionSugerida(val texto: String) : FormularioSenderoEvento
    data class AlAgregarServicioSugerido(val texto: String) : FormularioSenderoEvento
    data object AlCalcularDuracion : FormularioSenderoEvento
    data object AlUsarDistanciaDelMapa : FormularioSenderoEvento
    data object AlCalcularPerfil : FormularioSenderoEvento
    data class AlImportarTrazado(val uri: String) : FormularioSenderoEvento
    data object AlConfirmarReemplazoTrazado : FormularioSenderoEvento
    data object AlCancelarReemplazoTrazado : FormularioSenderoEvento
    // Caminata ya convertida: seguir creando el sendero de nuevo, que reemplaza al anterior
    data object AlReemplazarSenderoDeCaminata : FormularioSenderoEvento
    // Caminata ya convertida: abrir el sendero que ya existe para editarlo
    data object AlEditarSenderoDeCaminata : FormularioSenderoEvento
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
    data class EditarSenderoExistente(val idSendero: String) : FormularioSenderoEfecto
    data class MostrarAviso(val mensaje: String) : FormularioSenderoEfecto
}
