package com.unaj.senderos.core.ui.componentes.perfilelevacion

import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.ui.comun.formatearAlturaMetros
import com.unaj.senderos.core.ui.comun.formatearKilometros
import kotlin.math.roundToInt

const val TITULO_SECCION_PERFIL = "Perfil de elevación"
const val TEXTO_PERFIL_SIN_CALCULAR = "Todavía no se calculó el perfil"
const val TEXTO_PERFIL_DESACTUALIZADO = "El recorrido cambió desde que se calculó el perfil"
const val TEXTO_SUBIDA_RESTANTE = "Subida restante"

fun describirResumenPerfil(perfil: PerfilElevacion): String {
    return "$TITULO_SECCION_PERFIL: sube ${formatearAlturaMetros(perfil.desnivelPositivoMetros)} " +
        "y baja ${formatearAlturaMetros(perfil.desnivelNegativoMetros)}, " +
        "altura máxima ${formatearAlturaMetros(perfil.alturaMaximaMetros)}"
}

fun describirPuntoDelPerfil(kilometros: Double, altitudMetros: Double): String {
    return "Kilómetro ${formatearKilometros(kilometros)} · altura ${formatearAlturaMetros(altitudMetros.roundToInt())}"
}

fun describirPosicionActual(kilometros: Double, altitudMetros: Double): String {
    return "Estás acá: ${describirPuntoDelPerfil(kilometros, altitudMetros).replaceFirstChar { letra -> letra.lowercase() }}"
}
