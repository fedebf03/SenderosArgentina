package com.unaj.senderos.core.domain.model

data class PuntoPerfil(
    val distanciaKilometros: Double,
    val altitudMetros: Double
)

// La firma identifica el trazado con el que se calculó el perfil
data class PerfilElevacion(
    val puntos: List<PuntoPerfil>,
    val desnivelPositivoMetros: Int,
    val desnivelNegativoMetros: Int,
    val alturaMaximaMetros: Int,
    val alturaMinimaMetros: Int,
    val pendienteMaximaPorcentaje: Double,
    val firmaTrazado: String
)
