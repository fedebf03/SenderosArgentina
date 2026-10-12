package com.unaj.senderos.domain

private const val VELOCIDAD_MINIMA_METROS_POR_SEGUNDO = 0.5f
private const val ESPERA_SIN_PASOS_MILISEGUNDOS = 10_000L

object DetectorMovimiento {

    // Decidir si el usuario está detenido: velocidad GPS menor a 0,5 m/s (o desconocida) y, si el equipo mide pasos, 10 s sin pasos.
    // Sin sensor de pasos decide solo una velocidad conocida y baja: con velocidad desconocida se asume movimiento.
    // Con la señal débil (más de 15 s sin lectura válida, mayor que los 10 s de espera) y sin velocidad ni pasos se asume detenido.
    // El instante de referencia (inicio del recorrido) reemplaza al último paso mientras todavía no hubo ninguno
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
