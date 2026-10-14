package com.unaj.senderos.core.domain.model

// Altitud del terreno a una distancia acumulada del inicio del trazado
data class PuntoPerfil(
    val distanciaKilometros: Double,
    val altitudMetros: Double
)

// Perfil de elevación del trazado planificado de un sendero; la firma identifica el trazado con el que se calculó
data class PerfilElevacion(
    val puntos: List<PuntoPerfil>,
    val desnivelPositivoMetros: Int,
    val desnivelNegativoMetros: Int,
    val alturaMaximaMetros: Int,
    val alturaMinimaMetros: Int,
    val pendienteMaximaPorcentaje: Double,
    val firmaTrazado: String
)
