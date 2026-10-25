package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.domain.clima.AlertaClima
import com.unaj.senderos.core.domain.clima.EvaluadorAlertasClima
import com.unaj.senderos.core.domain.usecase.ResultadoPronostico
import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.recorrido.ConversorCaminata
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.ui.componentes.perfilelevacion.interpolarAltitud
import com.unaj.senderos.core.ui.revisionpuntos.RevisionPuntosEstadoVisual
import com.unaj.senderos.core.ui.revisionpuntos.RevisionPuntosEvento
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.ui.componentes.perfilelevacion.calcularKilometroEnTrazado
val TIPO_PUNTO_INICIAL: TipoPunto = TipoPunto.MIRADOR

val MULTIPLICADORES_SIMULACION: List<Int> = listOf(1, 5, 20)

enum class FaseRecorrido { INICIANDO, EN_CURSO, NO_ACTIVO }

data class ResumenRecorrido(
    val distanciaRecorridaKilometros: Double,
    val tiempoTranscurridoMilisegundos: Long
)

data class RecorridoEstadoVisual(
    val fase: FaseRecorrido = FaseRecorrido.INICIANDO,
    val sendero: Sendero? = null,
    val posicionUsuario: Coordenada? = null,
    // Posiciones caminadas hasta ahora, que se dibujan como la línea naranja a medida que avanza el recorrido
    val trazadoCaminado: List<Coordenada> = emptyList(),
    val rumboGrados: Float? = null,
    val estaSiguiendo: Boolean = true,
    val esSimulado: Boolean = false,
    val multiplicadorSimulacion: Int = 1,
    val distanciaRecorridaKilometros: Double = 0.0,
    val distanciaRestanteKilometros: Double = 0.0,
    val instanteInicioMilisegundos: Long = 0L,
    val proximoPunto: PuntoRecorrido? = null,
    val estaDesviado: Boolean = false,
    val esDialogoFinalizarVisible: Boolean = false,
    val resumen: ResumenRecorrido? = null,
    val nombreSendero: String = "",
    val llegoAlFinal: Boolean = false,
    val seDescartoLlegada: Boolean = false,
    val esDialogoLlegadaVisible: Boolean = false,
    val velocidadActualMetrosPorSegundo: Float? = null,
    val velocidadPromedioKilometrosPorHora: Double = 0.0,
    val pasos: Int? = null,
    val ritmoPasosPorMinuto: Double? = null,
    val estaDetenido: Boolean = false,
    val tieneAcelerometro: Boolean = true,
    val tieneBrujula: Boolean = true,
    val esDialogoCancelarVisible: Boolean = false,
    val calidadSenal: CalidadSenal = CalidadSenal.BUSCANDO,
    val esHojaPuntoVisible: Boolean = false,
    val indicePuntoSeleccionado: Int? = null,
    val tipoPuntoElegido: TipoPunto = TIPO_PUNTO_INICIAL,
    val notaPunto: String = "",
    val fotoPunto: String? = null,
    val estaGuardandoPunto: Boolean = false,
    val mensajeErrorPunto: String? = null,
    val revision: RevisionPuntosEstadoVisual = RevisionPuntosEstadoVisual(),
    val largoTrazadoKilometros: Double = 0.0,
    val perfilEstaDesactualizado: Boolean = false,
    // Una caminata libre no tiene trazado planificado, restante ni perfil.
    // Al finalizar se ofrece convertirla en un sendero nuevo
    val esCaminataLibre: Boolean = false,
    val clima: ResultadoPronostico? = null,
    // Es nula si el equipo no tiene barómetro o todavía no midió
    val presionHectopascales: Int? = null
) {
    // Se juntan los avisos del clima de ahora con los de hoy (el primer día del pronóstico)
    val alertasClima: Set<AlertaClima>
        get() = clima?.pronostico?.let { pronostico ->
            EvaluadorAlertasClima.evaluarActual(pronostico.actual) + pronostico.dias.firstOrNull()?.let { dia -> EvaluadorAlertasClima.evaluarDia(dia) }.orEmpty()
        }.orEmpty()

    val puedeAgregarPunto: Boolean
        get() = fase == FaseRecorrido.EN_CURSO

    // Solo una caminata libre terminada con al menos 50 m se puede convertir en un sendero nuevo
    val sePuedeConvertirEnSendero: Boolean
        get() = esCaminataLibre && resumen != null && ConversorCaminata.esConvertible(resumen.distanciaRecorridaKilometros)

    // Son los puntos marcados que todavía no se incorporaron a un sendero
    val puntosMarcados: List<PuntoRecorrido>
        get() = revision.puntosPendientes.map { conDistancia ->
            PuntoRecorrido(
                latitud = conDistancia.punto.latitud,
                longitud = conDistancia.punto.longitud,
                tipo = conDistancia.punto.tipo,
                nombre = conDistancia.punto.nota.trim(),
                foto = conDistancia.punto.foto
            )
        }

    val cantidadPuntosCapturados: Int
        get() = revision.puntosPendientes.size

    val sonConfiablesLosDatosDeDistancia: Boolean
        get() = calidadSenal == CalidadSenal.BUENA

    // Es nulo sin perfil vigente, sin señal confiable o antes de que el recorrido esté en curso
    val kilometroEnTrazado: Double?
        get() = if (
            fase == FaseRecorrido.EN_CURSO && sendero?.perfilElevacion != null &&
            !perfilEstaDesactualizado && sonConfiablesLosDatosDeDistancia
        ) {
            calcularKilometroEnTrazado(largoTrazadoKilometros, distanciaRestanteKilometros)
        } else {
            null
        }

    val subidaRestanteMetros: Int?
        get() {
            val perfil = sendero?.perfilElevacion ?: return null
            val kilometro = kilometroEnTrazado ?: return null
            return CalculadoraPerfil.calcularSubidaRestanteMetros(perfil, kilometro)
        }

    val altitudActualMetros: Double?
        get() {
            val perfil = sendero?.perfilElevacion ?: return null
            val kilometro = kilometroEnTrazado ?: return null
            return interpolarAltitud(perfil.puntos, kilometro)
        }

    // En simulación los pasos los estima el control
    val faltaAcelerometro: Boolean
        get() = !tieneAcelerometro && !esSimulado
}

sealed interface RecorridoEvento {
    data object AlVolver : RecorridoEvento
    data object AlTocarFinalizar : RecorridoEvento
    data object AlCancelarFinalizar : RecorridoEvento
    data object AlConfirmarFinalizar : RecorridoEvento
    data object AlTocarCancelarRecorrido : RecorridoEvento
    data object AlDescartarCancelacion : RecorridoEvento
    data object AlConfirmarCancelarRecorrido : RecorridoEvento
    data object AlCerrarResumen : RecorridoEvento
    data object AlSeguirCaminando : RecorridoEvento
    data object AlMoverMapaConGesto : RecorridoEvento
    data object AlTocarCentrar : RecorridoEvento
    data class AlSeleccionarPuntoEnMapa(val indice: Int?) : RecorridoEvento
    data class AlSeleccionarMultiplicador(val valor: Int) : RecorridoEvento
    data object AlTocarSimularDesvio : RecorridoEvento
    data object AlTocarAgregarPunto : RecorridoEvento
    data object AlCerrarHojaPunto : RecorridoEvento
    data class AlElegirTipoPunto(val tipo: TipoPunto) : RecorridoEvento
    data class AlCambiarNotaPunto(val nota: String) : RecorridoEvento
    data class AlSeleccionarFotoPunto(val uri: String) : RecorridoEvento
    data object AlQuitarFotoPunto : RecorridoEvento
    data class AlFallarFotoPunto(val mensaje: String) : RecorridoEvento
    data object AlGuardarPunto : RecorridoEvento
    data class AlRevisarPuntos(val evento: RevisionPuntosEvento) : RecorridoEvento
    data object AlConvertirEnSendero : RecorridoEvento
}

sealed interface RecorridoEfecto {
    data object VolverAlDetalle : RecorridoEfecto
    data class MostrarAviso(val mensaje: String) : RecorridoEfecto
    data class NavegarAFormularioDesdeCaminata(val idCaminata: String) : RecorridoEfecto
}
