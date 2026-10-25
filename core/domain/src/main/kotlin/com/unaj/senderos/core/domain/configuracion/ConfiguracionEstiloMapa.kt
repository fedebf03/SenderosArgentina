package com.unaj.senderos.core.domain.configuracion

// Están en dominio porque las usan tanto la presentación como la capa de datos
object ConfiguracionEstiloMapa {
    const val URL_ESTILO = "https://tiles.openfreemap.org/styles/liberty"

    const val ZOOM_MINIMO_DESCARGA = 10.0
    const val ZOOM_MAXIMO_DESCARGA = 16.0
}
