package com.unaj.senderos.core.ui.componentes

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

/** Obtener el nombre visible de un bioma. */
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

/** Obtener la descripción del bioma para lectores de pantalla. */
fun obtenerDescripcionBioma(bioma: Bioma): String {
    return "Bioma: ${obtenerTextoBioma(bioma).lowercase()}"
}
