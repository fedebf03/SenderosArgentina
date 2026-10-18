package com.unaj.senderos.core.domain.clima

// Avisos que se le muestran al usuario antes de salir a caminar
enum class AlertaClima(val titulo: String, val detalle: String) {
    VIENTO_FUERTE("Viento fuerte", "Hay viento sostenido de 40 km/h o ráfagas de 60 km/h o más: evitá las crestas y los tramos expuestos"),
    TORMENTA("Tormenta", "Se esperan tormentas: no salgas a zonas altas ni abiertas y buscá refugio ante los primeros truenos"),
    LLUVIA_INTENSA("Lluvia intensa", "Se esperan lluvias fuertes: los senderos pueden estar resbaladizos y los arroyos crecidos"),
    FRIO_EXTREMO("Frío extremo", "La temperatura mínima baja de -5 °C: llevá abrigo adicional y cuidá tus manos y pies"),
    CALOR_EXTREMO("Calor extremo", "La temperatura máxima llega a 35 °C o más: llevá agua de más y evitá caminar en las horas centrales")
}

private const val VIENTO_FUERTE_KILOMETROS_POR_HORA = 40.0
private const val RAFAGAS_FUERTES_KILOMETROS_POR_HORA = 60.0
private const val LLUVIA_INTENSA_MILIMETROS = 20.0
private const val FRIO_EXTREMO_CELSIUS = -5.0
private const val CALOR_EXTREMO_CELSIUS = 35.0

object EvaluadorAlertasClima {

    // Evaluar los avisos de un día del pronóstico con los umbrales de viento, tormenta, lluvia y temperatura
    fun evaluarDia(dia: PronosticoDia): Set<AlertaClima> = buildSet {
        if (esVientoFuerte(dia.vientoMaximoKilometrosPorHora, dia.rafagasMaximasKilometrosPorHora)) add(AlertaClima.VIENTO_FUERTE)
        if (esTormenta(dia.condicion)) add(AlertaClima.TORMENTA)
        if (dia.precipitacionMilimetros >= LLUVIA_INTENSA_MILIMETROS || dia.condicion == CondicionClima.LLUVIA_FUERTE) add(AlertaClima.LLUVIA_INTENSA)
        if (dia.temperaturaMinimaCelsius <= FRIO_EXTREMO_CELSIUS) add(AlertaClima.FRIO_EXTREMO)
        if (dia.temperaturaMaximaCelsius >= CALOR_EXTREMO_CELSIUS) add(AlertaClima.CALOR_EXTREMO)
    }

    // Evaluar los avisos del clima de ahora: viento y tormenta, que son los que cambian la decisión de salir en este momento
    fun evaluarActual(actual: ClimaActual): Set<AlertaClima> = buildSet {
        if (esVientoFuerte(actual.vientoKilometrosPorHora, actual.rafagasKilometrosPorHora)) add(AlertaClima.VIENTO_FUERTE)
        if (esTormenta(actual.condicion)) add(AlertaClima.TORMENTA)
    }

    // Agrupar por fecha los avisos de cada día del pronóstico; los días tranquilos no aparecen
    fun evaluarDias(pronostico: PronosticoClima): Map<String, Set<AlertaClima>> =
        pronostico.dias
            .associate { dia -> dia.fecha to evaluarDia(dia) }
            .filterValues { avisos -> avisos.isNotEmpty() }

    private fun esVientoFuerte(vientoKilometrosPorHora: Double, rafagasKilometrosPorHora: Double?): Boolean =
        vientoKilometrosPorHora >= VIENTO_FUERTE_KILOMETROS_POR_HORA ||
            (rafagasKilometrosPorHora != null && rafagasKilometrosPorHora >= RAFAGAS_FUERTES_KILOMETROS_POR_HORA)

    private fun esTormenta(condicion: CondicionClima): Boolean =
        condicion == CondicionClima.TORMENTA || condicion == CondicionClima.TORMENTA_CON_GRANIZO
}
