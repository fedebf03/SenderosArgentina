package com.unaj.senderos.features.senderos.formulario

import java.text.Normalizer

// Lo que más se repite al cargar un sendero, para agregarlo con un toque en lugar de escribirlo
val RECOMENDACIONES_SUGERIDAS: List<String> = listOf(
    "Llevar agua suficiente",
    "Revisar el pronóstico antes de salir",
    "Avisar a alguien la ruta y la hora de regreso",
    "Calzado adecuado",
    "Ropa de abrigo",
    "Protector solar",
    "Botiquín básico",
    "Linterna",
    "Descargar el mapa sin conexión",
    "No dejar residuos",
    "Respetar la flora y la fauna"
)

// Un sendero nuevo arranca con las tres primeras, que se pueden quitar
val RECOMENDACIONES_POR_DEFECTO: List<String> = RECOMENDACIONES_SUGERIDAS.take(3)

val SERVICIOS_SUGERIDOS: List<String> = listOf(
    "Estacionamiento",
    "Baños",
    "Agua potable",
    "Camping",
    "Refugio",
    "Guardaparques",
    "Proveeduría",
    "Señal de celular",
    "Alquiler de equipo"
)

val EPOCAS_SUGERIDAS: List<String> = listOf(
    "Todo el año",
    "Primavera",
    "Verano",
    "Otoño",
    "Invierno",
    "Primavera a otoño",
    "Verano y otoño"
)

private const val MINUTOS_POR_HORA = 60
private val MARCAS_DE_TILDE = Regex("\\p{Mn}+")

fun formatearDuracionEstimada(minutos: Int): String {
    val horas = minutos / MINUTOS_POR_HORA
    val resto = minutos % MINUTOS_POR_HORA
    return when {
        horas == 0 -> "$resto min"
        resto == 0 -> "$horas h"
        else -> "$horas h $resto min"
    }
}

fun filtrarSugerencias(opciones: List<String>, texto: String): List<String> {
    val buscado = normalizarParaBuscar(texto.trim())
    if (buscado.isEmpty()) return opciones
    return opciones.filter { opcion -> normalizarParaBuscar(opcion).contains(buscado) }
}

// Se quitan las tildes para que «rio» coincida con «Río»
private fun normalizarParaBuscar(texto: String): String {
    val descompuesto = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
    return descompuesto.replace(MARCAS_DE_TILDE, "")
}
