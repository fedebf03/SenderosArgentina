package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraRumbo
import com.unaj.senderos.core.ui.comun.TEXTO_SIN_DATO
import com.unaj.senderos.core.ui.sendero.obtenerTextoTipoPunto
import kotlin.math.roundToInt

private const val GRADOS_CIRCUNFERENCIA = 360

fun formatearRumbo(grados: Float?): String {
    if (grados == null) return TEXTO_SIN_DATO
    val gradosEnteros = CalculadoraRecorrido.normalizarRumbo(grados).roundToInt() % GRADOS_CIRCUNFERENCIA
    return "${CalculadoraRumbo.aPuntoCardinal(grados).simbolo} · $gradosEnteros°"
}

fun obtenerEtiquetaPunto(punto: PuntoRecorrido): String {
    return punto.nombre.ifBlank { obtenerTextoTipoPunto(punto.tipo) }
}

fun describirAviso(aviso: AvisoRecorrido): String {
    return when (aviso) {
        is AvisoRecorrido.CercaDePunto -> "Cerca de ${obtenerEtiquetaPunto(aviso.punto)}"
        AvisoRecorrido.Desvio -> "Te alejaste del sendero"
        AvisoRecorrido.LlegadaAlFinal -> "¡Llegaste al final!"
    }
}

fun describirMotivoFin(motivo: MotivoFinSeguimiento): String {
    return when (motivo) {
        MotivoFinSeguimiento.PERMISO_DE_UBICACION_REVOCADO -> "Se revocó el permiso de ubicación"
        MotivoFinSeguimiento.ERROR_DE_UBICACION -> "No se pudo obtener la ubicación"
        MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO -> "El recorrido fue cancelado"
    }
}
