package com.unaj.senderos.core.domain.sensores

private const val VELOCIDAD_MINIMA_METROS_POR_SEGUNDO = 0.5f
private const val ESPERA_SIN_PASOS_MILISEGUNDOS = 10_000L

object DetectorMovimiento {

    // Sin sensor de pasos solo decide una velocidad conocida y baja. Con la señal débil (más de 15 s sin lectura
    // válida) y sin velocidad ni pasos se asume que el usuario está detenido
    fun estaDetenido(
        velocidadMetrosPorSegundo: Float?,
        instanteUltimoPaso: Long?,
        instanteReferencia: Long,
        ahora: Long,
        midePasos: Boolean,
        senalDebil: Boolean = false
    ): Boolean {
        val velocidadBaja = velocidadMetrosPorSegundo == null || velocidadMetrosPorSegundo < VELOCIDAD_MINIMA_METROS_POR_SEGUNDO
        if (!midePasos) return if (velocidadMetrosPorSegundo != null) velocidadBaja else senalDebil
        val sinPasos = ahora - (instanteUltimoPaso ?: instanteReferencia) >= ESPERA_SIN_PASOS_MILISEGUNDOS
        return velocidadBaja && sinPasos
    }
}
