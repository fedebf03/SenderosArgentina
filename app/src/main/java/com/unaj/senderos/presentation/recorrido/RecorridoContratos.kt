package com.unaj.senderos.presentation.recorrido

import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero

// Velocidades de avance que admite la simulación
val MULTIPLICADORES_SIMULACION: List<Int> = listOf(1, 5, 20)

// Fase de la pantalla: esperando al servicio, recorrido activo o sin recorrido activo
enum class FaseRecorrido { INICIANDO, EN_CURSO, NO_ACTIVO }

// Distancia y tiempo capturados al finalizar el recorrido
data class ResumenRecorrido(
    val distanciaRecorridaKilometros: Double,
    val tiempoTranscurridoMilisegundos: Long
)

data class RecorridoEstadoVisual(
    val fase: FaseRecorrido = FaseRecorrido.INICIANDO,
    val sendero: Sendero? = null,
    val posicionUsuario: Coordenada? = null,
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
    val calidadSenal: CalidadSenal = CalidadSenal.BUSCANDO
) {
    // Indicar si las distancias y velocidades merecen confianza: solo con buena señal
    val sonConfiablesLosDatosDeDistancia: Boolean
        get() = calidadSenal == CalidadSenal.BUENA

    // Indicar que no hay acelerómetro que medir; en simulación los pasos los estima el control
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
    data class AlSeleccionarMultiplicador(val valor: Int) : RecorridoEvento
    data object AlTocarSimularDesvio : RecorridoEvento
}

sealed interface RecorridoEfecto {
    data object VolverAlDetalle : RecorridoEfecto
    data class MostrarAviso(val mensaje: String) : RecorridoEfecto
}
