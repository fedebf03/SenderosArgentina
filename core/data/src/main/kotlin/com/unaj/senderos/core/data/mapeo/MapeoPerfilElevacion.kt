package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.database.sendero.PerfilElevacionEntidad
import com.unaj.senderos.core.database.sendero.PuntoPerfilEntidad
import com.unaj.senderos.core.network.sendero.PerfilElevacionRed
import com.unaj.senderos.core.network.sendero.PuntoPerfilRed
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil

fun PerfilElevacion.aEntidad(): PerfilElevacionEntidad {
    return PerfilElevacionEntidad(
        puntos = puntos.map { punto -> PuntoPerfilEntidad(punto.distanciaKilometros, punto.altitudMetros) },
        desnivelPositivoMetros = desnivelPositivoMetros,
        desnivelNegativoMetros = desnivelNegativoMetros,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        pendienteMaximaPorcentaje = pendienteMaximaPorcentaje,
        firmaTrazado = firmaTrazado
    )
}

fun PerfilElevacionEntidad.aDominio(): PerfilElevacion {
    return PerfilElevacion(
        puntos = puntos.map { punto -> PuntoPerfil(punto.distanciaKilometros, punto.altitudMetros) },
        desnivelPositivoMetros = desnivelPositivoMetros,
        desnivelNegativoMetros = desnivelNegativoMetros,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        pendienteMaximaPorcentaje = pendienteMaximaPorcentaje,
        firmaTrazado = firmaTrazado
    )
}

fun PerfilElevacionEntidad.aRed(): PerfilElevacionRed {
    return PerfilElevacionRed(
        puntos = puntos.map { punto -> PuntoPerfilRed(punto.distanciaKilometros, punto.altitudMetros) },
        desnivelPositivoMetros = desnivelPositivoMetros,
        desnivelNegativoMetros = desnivelNegativoMetros,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        pendienteMaximaPorcentaje = pendienteMaximaPorcentaje,
        firmaTrazado = firmaTrazado
    )
}

fun PerfilElevacionRed.aEntidad(): PerfilElevacionEntidad {
    return PerfilElevacionEntidad(
        puntos = puntos.map { punto -> PuntoPerfilEntidad(punto.distanciaKilometros, punto.altitudMetros) },
        desnivelPositivoMetros = desnivelPositivoMetros,
        desnivelNegativoMetros = desnivelNegativoMetros,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        pendienteMaximaPorcentaje = pendienteMaximaPorcentaje,
        firmaTrazado = firmaTrazado
    )
}
