package com.unaj.senderos.presentation.recorrido

import com.unaj.senderos.domain.CalculadoraRecorrido
import com.unaj.senderos.domain.CalculadoraRumbo
import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.presentation.formulario.obtenerTextoTipoPunto
import java.util.Locale
import kotlin.math.roundToInt

private const val TEXTO_SIN_DATO = "—"
private const val KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO = 3.6f
private const val GRADOS_CIRCUNFERENCIA = 360
private val LOCALIDAD_ARGENTINA: Locale = Locale.forLanguageTag("es-AR")
private const val MILISEGUNDOS_POR_SEGUNDO = 1_000L
private const val SEGUNDOS_POR_MINUTO = 60L
private const val SEGUNDOS_POR_HORA = 3_600L

// Dar formato mm:ss, o h:mm:ss desde la primera hora, a un tiempo en milisegundos
fun formatearTiempoTranscurrido(milisegundos: Long): String {
    val segundosTotales = (milisegundos.coerceAtLeast(0L)) / MILISEGUNDOS_POR_SEGUNDO
    val horas = segundosTotales / SEGUNDOS_POR_HORA
    val minutos = (segundosTotales % SEGUNDOS_POR_HORA) / SEGUNDOS_POR_MINUTO
    val segundos = segundosTotales % SEGUNDOS_POR_MINUTO
    return if (horas > 0) {
        String.format(Locale.US, "%d:%02d:%02d", horas, minutos, segundos)
    } else {
        String.format(Locale.US, "%02d:%02d", minutos, segundos)
    }
}

// Dar formato en metros (menos de 1 km) o en kilómetros con coma decimal a una distancia
fun formatearDistanciaRecorrido(kilometros: Double): String {
    if (kilometros < 1.0) {
        return "${(kilometros * 1_000).roundToInt()} m"
    }
    return String.format(Locale.forLanguageTag("es-AR"), "%.1f km", kilometros)
}

// Dar formato "5,4 km/h" a la velocidad actual en metros por segundo, o "—" si no se conoce
fun formatearVelocidad(metrosPorSegundo: Float?): String {
    if (metrosPorSegundo == null) return TEXTO_SIN_DATO
    return String.format(LOCALIDAD_ARGENTINA, "%.1f km/h", metrosPorSegundo * KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO)
}

// Dar formato "3,2 km/h" a la velocidad promedio en kilómetros por hora
fun formatearVelocidadPromedio(kilometrosPorHora: Double): String {
    return String.format(LOCALIDAD_ARGENTINA, "%.1f km/h", kilometrosPorHora)
}

// Dar formato "NE · 45°" a un rumbo como punto cardinal y grados enteros, o "—" si no se conoce
fun formatearRumbo(grados: Float?): String {
    if (grados == null) return TEXTO_SIN_DATO
    val gradosEnteros = CalculadoraRecorrido.normalizarRumbo(grados).roundToInt() % GRADOS_CIRCUNFERENCIA
    return "${CalculadoraRumbo.aPuntoCardinal(grados).simbolo} · $gradosEnteros°"
}

// Dar formato con separador de miles argentino a los pasos, o "—" si no se conocen
fun formatearPasosDelPanel(pasos: Int?): String {
    return if (pasos == null) TEXTO_SIN_DATO else String.format(LOCALIDAD_ARGENTINA, "%,d", pasos)
}

// Dar formato "95 pasos/min" al ritmo, o "—" si no se conoce
fun formatearRitmoDelPanel(pasosPorMinuto: Double?): String {
    return if (pasosPorMinuto == null) TEXTO_SIN_DATO else "${pasosPorMinuto.roundToInt()} pasos/min"
}

// Obtener el nombre del punto o, si no tiene, el texto de su tipo
fun obtenerEtiquetaPunto(punto: PuntoRecorrido): String {
    return punto.nombre.ifBlank { obtenerTextoTipoPunto(punto.tipo) }
}

// Redactar el aviso del seguimiento para mostrarlo en pantalla
fun describirAviso(aviso: AvisoRecorrido): String {
    return when (aviso) {
        is AvisoRecorrido.CercaDePunto -> "Cerca de ${obtenerEtiquetaPunto(aviso.punto)}"
        AvisoRecorrido.Desvio -> "Te alejaste del sendero"
        AvisoRecorrido.LlegadaAlFinal -> "¡Llegaste al final!"
    }
}

// Redactar el motivo por el que el seguimiento terminó sin que el usuario lo pidiera
fun describirMotivoFin(motivo: MotivoFinSeguimiento): String {
    return when (motivo) {
        MotivoFinSeguimiento.PERMISO_DE_UBICACION_REVOCADO -> "Se revocó el permiso de ubicación"
        MotivoFinSeguimiento.ERROR_DE_UBICACION -> "No se pudo obtener la ubicación"
        MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO -> "El recorrido fue cancelado"
    }
}
