package com.unaj.senderos.core.domain.configuracion

// Constantes puras del mapa base; viven en dominio porque tanto presentation como data pueden depender de él
object ConfiguracionEstiloMapa {
    // Estilo vectorial gratuito de OpenFreeMap (sin clave); MapLibre agrega la atribución automáticamente
    const val URL_ESTILO = "https://tiles.openfreemap.org/styles/liberty"

    const val ZOOM_MINIMO_DESCARGA = 10.0
    const val ZOOM_MAXIMO_DESCARGA = 16.0
}
