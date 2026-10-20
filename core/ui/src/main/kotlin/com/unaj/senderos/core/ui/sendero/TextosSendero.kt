package com.unaj.senderos.core.ui.sendero

import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.TipoRecorrido

fun obtenerTextoDificultad(dificultad: Dificultad): String {
    return when (dificultad) {
        Dificultad.BAJA -> "Baja"
        Dificultad.MEDIA -> "Media"
        Dificultad.ALTA -> "Alta"
    }
}

fun obtenerTextoTipoRecorrido(tipoRecorrido: TipoRecorrido): String {
    return when (tipoRecorrido) {
        TipoRecorrido.CIRCULAR -> "Circular"
        TipoRecorrido.IDA_Y_VUELTA -> "Ida y vuelta"
        TipoRecorrido.LINEAL -> "Lineal"
    }
}

fun obtenerTextoBioma(bioma: Bioma): String {
    return when (bioma) {
        Bioma.ALTA_MONTANIA -> "Alta montaña"
        Bioma.BOSQUE_ANDINO_PATAGONICO -> "Bosque andino patagónico"
        Bioma.ESTEPA_PATAGONICA -> "Estepa patagónica"
        Bioma.PUNA -> "Puna"
        Bioma.YUNGAS -> "Yungas"
        Bioma.MONTE -> "Monte"
        Bioma.SIERRAS -> "Sierras"
        Bioma.LITORAL -> "Litoral"
        Bioma.COSTA_MARITIMA -> "Costa marítima"
        Bioma.SIN_DEFINIR -> "Sin definir"
    }
}

fun obtenerDescripcionBioma(bioma: Bioma): String {
    return "Bioma: ${obtenerTextoBioma(bioma).lowercase()}"
}
