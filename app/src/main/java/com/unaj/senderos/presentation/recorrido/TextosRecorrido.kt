package com.unaj.senderos.presentation.recorrido

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.presentation.formulario.obtenerTextoTipoPunto
import java.util.Locale
import kotlin.math.roundToInt

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
    }
}
