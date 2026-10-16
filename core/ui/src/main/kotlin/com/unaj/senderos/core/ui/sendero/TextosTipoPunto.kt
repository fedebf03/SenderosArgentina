package com.unaj.senderos.core.ui.sendero

import com.unaj.senderos.core.domain.model.TipoPunto

fun obtenerTextoTipoPunto(tipo: TipoPunto): String {
    return when (tipo) {
        TipoPunto.PASO -> "Paso"
        TipoPunto.MIRADOR -> "Mirador"
        TipoPunto.AGUA -> "Agua"
        TipoPunto.REFUGIO -> "Refugio"
        TipoPunto.CAMPING -> "Camping"
        TipoPunto.PELIGRO -> "Peligro"
        TipoPunto.OTRO -> "Otro"
    }
}
