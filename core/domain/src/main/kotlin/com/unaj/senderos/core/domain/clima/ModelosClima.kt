package com.unaj.senderos.core.domain.clima

import com.unaj.senderos.core.domain.model.Coordenada

// Se arma a partir de los códigos WMO que informa Open-Meteo
enum class CondicionClima(val descripcion: String) {
    DESPEJADO("Despejado"),
    POCO_NUBLADO("Poco nublado"),
    PARCIALMENTE_NUBLADO("Parcialmente nublado"),
    NUBLADO("Nublado"),
    NIEBLA("Niebla"),
    LLOVIZNA("Llovizna"),
    LLUVIA("Lluvia"),
    LLUVIA_FUERTE("Lluvia fuerte"),
    CHUBASCOS("Chubascos"),
    NIEVE("Nieve"),
    TORMENTA("Tormenta"),
    TORMENTA_CON_GRANIZO("Tormenta con granizo"),
    VARIABLE("Variable");

    companion object {
        fun desdeCodigo(codigo: Int): CondicionClima = when (codigo) {
            0 -> DESPEJADO
            1 -> POCO_NUBLADO
            2 -> PARCIALMENTE_NUBLADO
            3 -> NUBLADO
            45, 48 -> NIEBLA
            51, 53, 55, 56, 57 -> LLOVIZNA
            61, 63, 66 -> LLUVIA
            65, 67 -> LLUVIA_FUERTE
            71, 73, 75, 77, 85, 86 -> NIEVE
            80, 81, 82 -> CHUBASCOS
            95 -> TORMENTA
            96, 99 -> TORMENTA_CON_GRANIZO
            else -> VARIABLE
        }
    }
}

data class ClimaActual(
    val temperaturaCelsius: Double,
    val sensacionTermicaCelsius: Double?,
    val vientoKilometrosPorHora: Double,
    val rafagasKilometrosPorHora: Double?,
    val condicion: CondicionClima,
    val esDeDia: Boolean
)

// La fecha va como texto ISO (aaaa-mm-dd) en la zona horaria del lugar
data class PronosticoDia(
    val fecha: String,
    val temperaturaMaximaCelsius: Double,
    val temperaturaMinimaCelsius: Double,
    val probabilidadLluviaPorcentaje: Int?,
    val precipitacionMilimetros: Double,
    val vientoMaximoKilometrosPorHora: Double,
    val rafagasMaximasKilometrosPorHora: Double,
    val condicion: CondicionClima
)

data class PronosticoClima(
    val lugar: Coordenada,
    val obtenidoMilisegundos: Long,
    val actual: ClimaActual,
    val dias: List<PronosticoDia>
)
