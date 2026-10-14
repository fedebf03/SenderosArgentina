package com.unaj.senderos.core.database

import kotlinx.serialization.Serializable

@Serializable
data class PuntoPerfilEntidad(
    val distanciaKilometros: Double,
    val altitudMetros: Double
)

// Perfil de elevación guardado como JSON en la columna perfilElevacion de senderos
@Serializable
data class PerfilElevacionEntidad(
    val puntos: List<PuntoPerfilEntidad>,
    val desnivelPositivoMetros: Int,
    val desnivelNegativoMetros: Int,
    val alturaMaximaMetros: Int,
    val alturaMinimaMetros: Int,
    val pendienteMaximaPorcentaje: Double,
    val firmaTrazado: String
)
