package com.unaj.senderos.core.network.sendero

// Los valores por defecto los necesita el deserializador de Firestore
data class PuntoPerfilRed(
    val distanciaKilometros: Double = 0.0,
    val altitudMetros: Double = 0.0
)

data class PerfilElevacionRed(
    val puntos: List<PuntoPerfilRed> = emptyList(),
    val desnivelPositivoMetros: Int = 0,
    val desnivelNegativoMetros: Int = 0,
    val alturaMaximaMetros: Int = 0,
    val alturaMinimaMetros: Int = 0,
    val pendienteMaximaPorcentaje: Double = 0.0,
    val firmaTrazado: String = ""
)
