package com.unaj.senderos.core.network

// Punto del perfil como se guarda en Firestore (valores por defecto requeridos por el deserializador)
data class PuntoPerfilRed(
    val distanciaKilometros: Double = 0.0,
    val altitudMetros: Double = 0.0
)

// Perfil de elevación como se guarda en Firestore dentro del documento del sendero
data class PerfilElevacionRed(
    val puntos: List<PuntoPerfilRed> = emptyList(),
    val desnivelPositivoMetros: Int = 0,
    val desnivelNegativoMetros: Int = 0,
    val alturaMaximaMetros: Int = 0,
    val alturaMinimaMetros: Int = 0,
    val pendienteMaximaPorcentaje: Double = 0.0,
    val firmaTrazado: String = ""
)
