package com.unaj.senderos.core.ui.componentes.mapa

import com.unaj.senderos.core.domain.configuracion.ConfiguracionEstiloMapa

object ConfiguracionMapa {
    // El estilo vive en dominio para compartirlo con la descarga de mapas sin conexión
    const val URL_ESTILO = ConfiguracionEstiloMapa.URL_ESTILO

    const val FUENTE_TEXTO = "Noto Sans Bold"

    const val LATITUD_INICIAL = -40.5
    const val LONGITUD_INICIAL = -71.3
    const val ZOOM_INICIAL = 6.0

    const val ZOOM_UN_PUNTO = 14.0

    const val ZOOM_PUNTO_DE_INTERES = 16.0

    // Fracción superior libre para ver más camino adelante
    const val ZOOM_NAVEGACION = 17.5
    const val INCLINACION_NAVEGACION_GRADOS = 45.0
    const val FRACCION_MARGEN_SUPERIOR_NAVEGACION = 0.4
    const val DURACION_ANIMACION_NAVEGACION_MILISEGUNDOS = 1000

    const val MARGEN_ENCUADRE_DP = 48
    const val RADIO_CAPTURA_MARCADOR_DP = 28
}
