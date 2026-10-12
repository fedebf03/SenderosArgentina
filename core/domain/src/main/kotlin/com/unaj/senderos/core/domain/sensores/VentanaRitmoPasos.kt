package com.unaj.senderos.domain

private const val VENTANA_MILISEGUNDOS = 10_000L
private const val SEPARACION_MAXIMA_MILISEGUNDOS = 2_000L
private const val MILISEGUNDOS_POR_MINUTO = 60_000.0

// Calcular el ritmo en pasos por minuto sobre una ventana deslizante de 10 s; si el último paso fue hace más de 2 s ya no se camina y el ritmo es 0
class VentanaRitmoPasos {

    private val instantesDePasos = ArrayDeque<Long>()

    // Registrar un paso en el instante indicado y descartar los que salieron de la ventana
    fun registrarPaso(instanteMilisegundos: Long) {
        instantesDePasos.addLast(instanteMilisegundos)
        descartarViejos(instanteMilisegundos)
    }

    // Calcular el ritmo de los pasos dados en los últimos 10 s; hacen falta al menos dos pasos
    fun ritmoPasosPorMinuto(ahoraMilisegundos: Long): Double {
        val recientes = instantesDePasos.filter { instante -> ahoraMilisegundos - instante <= VENTANA_MILISEGUNDOS }
        val ultimo = recientes.lastOrNull() ?: return 0.0
        if (recientes.size < 2 || ahoraMilisegundos - ultimo > SEPARACION_MAXIMA_MILISEGUNDOS) return 0.0
        val duracion = ultimo - recientes.first()
        if (duracion <= 0L) return 0.0
        return (recientes.size - 1) * MILISEGUNDOS_POR_MINUTO / duracion
    }

    // Borrar todos los pasos registrados
    fun reiniciar() {
        instantesDePasos.clear()
    }

    // Descartar los pasos anteriores a la ventana respecto del instante indicado
    private fun descartarViejos(ahoraMilisegundos: Long) {
        while (instantesDePasos.isNotEmpty() && ahoraMilisegundos - instantesDePasos.first() > VENTANA_MILISEGUNDOS) {
            instantesDePasos.removeFirst()
        }
    }
}
