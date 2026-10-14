package com.unaj.senderos.core.domain.configuracion

// Constantes puras del perfil de elevación; viven en dominio porque tanto presentation como data pueden depender de él
object ConfiguracionElevacion {
    // Atribución obligatoria de la fuente de alturas, para mostrar junto al gráfico del perfil
    const val TEXTO_ATRIBUCION = "Elevación: Copernicus DEM GLO-90 vía Open-Meteo"
}
