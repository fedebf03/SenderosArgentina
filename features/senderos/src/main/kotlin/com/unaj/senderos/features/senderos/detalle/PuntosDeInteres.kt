package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.domain.model.Coordenada

internal fun DetalleSenderoEstadoVisual.coordenadaDelPunto(indice: Int): Coordenada? {
    val punto = sendero?.recorrido?.getOrNull(indice) ?: return null
    return Coordenada(punto.latitud, punto.longitud)
}
