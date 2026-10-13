package com.unaj.senderos.presentation.recorrido

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.domain.recorrido.CalculadoraRumbo
import com.unaj.senderos.presentation.comun.TEXTO_SIN_DATO
import com.unaj.senderos.presentation.formulario.obtenerTextoTipoPunto
import java.util.Locale
import kotlin.math.roundToInt

private const val GRADOS_CIRCUNFERENCIA = 360
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

// Dar formato "NE · 45°" a un rumbo como punto cardinal y grados enteros, o "—" si no se conoce
fun formatearRumbo(grados: Float?): String {
    if (grados == null) return TEXTO_SIN_DATO
    val gradosEnteros = CalculadoraRecorrido.normalizarRumbo(grados).roundToInt() % GRADOS_CIRCUNFERENCIA
    return "${CalculadoraRumbo.aPuntoCardinal(grados).simbolo} · $gradosEnteros°"
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
