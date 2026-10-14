package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.database.PerfilElevacionEntidad
import com.unaj.senderos.core.database.PuntoPerfilEntidad
import com.unaj.senderos.core.network.PerfilElevacionRed
import com.unaj.senderos.core.network.PuntoPerfilRed
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil

// Convertir el perfil de dominio a su modelo local
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

// Convertir el perfil local al modelo de dominio
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

// Convertir el perfil local al documento de Firestore
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

// Convertir el perfil del documento de Firestore al modelo local
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
