package com.unaj.senderos.domain

private const val VELOCIDAD_MINIMA_METROS_POR_SEGUNDO = 0.5f
private const val ESPERA_SIN_PASOS_MILISEGUNDOS = 10_000L

object DetectorMovimiento {

    // Decidir si el usuario está detenido: velocidad GPS menor a 0,5 m/s (o desconocida) y, si el equipo mide pasos, 10 s sin pasos.
    // Sin sensor de pasos decide solo una velocidad conocida y baja: con velocidad desconocida se asume movimiento.
    // El instante de referencia (inicio del recorrido) reemplaza al último paso mientras todavía no hubo ninguno
    fun estaDetenido(
        velocidadMetrosPorSegundo: Float?,
        instanteUltimoPaso: Long?,
        instanteReferencia: Long,
        ahora: Long,
        midePasos: Boolean
    ): Boolean {
        val velocidadBaja = velocidadMetrosPorSegundo == null || velocidadMetrosPorSegundo < VELOCIDAD_MINIMA_METROS_POR_SEGUNDO
        if (!midePasos) return velocidadMetrosPorSegundo != null && velocidadBaja
        val sinPasos = ahora - (instanteUltimoPaso ?: instanteReferencia) >= ESPERA_SIN_PASOS_MILISEGUNDOS
        return velocidadBaja && sinPasos
    }
}
