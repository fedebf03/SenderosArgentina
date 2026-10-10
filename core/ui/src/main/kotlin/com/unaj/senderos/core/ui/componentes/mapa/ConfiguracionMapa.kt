package com.unaj.senderos.presentation.mapa

// Constantes configurables del mapa; cambiar de proveedor de teselas solo requiere editar la URL del estilo
object ConfiguracionMapa {
    // Estilo vectorial gratuito de OpenFreeMap (sin clave); MapLibre agrega la atribución automáticamente
    const val URL_ESTILO = "https://tiles.openfreemap.org/styles/liberty"

    // Fuente tipográfica disponible en los glifos del estilo de OpenFreeMap
    const val FUENTE_TEXTO = "Noto Sans Bold"

    // Vista inicial sin puntos: Patagonia andina
    const val LATITUD_INICIAL = -40.5
    const val LONGITUD_INICIAL = -71.3
    const val ZOOM_INICIAL = 6.0

    // Zoom al encuadrar un único punto
    const val ZOOM_UN_PUNTO = 14.0

    // Margen del encuadre en dp y radio de captura de un marcador al tocar en dp
    const val MARGEN_ENCUADRE_DP = 48
    const val RADIO_CAPTURA_MARCADOR_DP = 28
}
