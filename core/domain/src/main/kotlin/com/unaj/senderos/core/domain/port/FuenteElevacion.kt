package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada

interface FuenteElevacion {
    suspend fun obtenerAltitudes(coordenadas: List<Coordenada>): Result<List<Double>>
}

// esLimitacionDelServicio marca un límite o una caída (429, 5xx) donde insistir no sirve. En cambio esRechazoDefinitivo
// marca un rechazo del pedido (otro 4xx) que daría el mismo error al repetirlo
open class ErrorElevacionException(
    mensaje: String,
    causa: Throwable? = null,
    val esLimitacionDelServicio: Boolean = false,
    val esRechazoDefinitivo: Boolean = false
) : Exception(mensaje, causa)

class SinConexionElevacionException(causa: Throwable?) :
    ErrorElevacionException("Sin conexión para calcular el perfil", causa)
