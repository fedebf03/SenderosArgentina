package com.unaj.senderos.presentation.componentes

import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.TipoRecorrido

// Obtener el texto en español de una dificultad
fun obtenerTextoDificultad(dificultad: Dificultad): String {
    return when (dificultad) {
        Dificultad.BAJA -> "Baja"
        Dificultad.MEDIA -> "Media"
        Dificultad.ALTA -> "Alta"
    }
}

// Obtener el texto en español de un tipo de recorrido
fun obtenerTextoTipoRecorrido(tipoRecorrido: TipoRecorrido): String {
    return when (tipoRecorrido) {
        TipoRecorrido.CIRCULAR -> "Circular"
        TipoRecorrido.IDA_Y_VUELTA -> "Ida y vuelta"
        TipoRecorrido.LINEAL -> "Lineal"
    }
}
