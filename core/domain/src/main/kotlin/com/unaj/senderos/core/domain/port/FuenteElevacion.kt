package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada

interface FuenteElevacion {
    suspend fun obtenerAltitudes(coordenadas: List<Coordenada>): Result<List<Double>>
}

// Si el servicio está saturado o caído (429 o 5xx), esLimitacionDelServicio frena los pedidos hasta la próxima
// sincronización. Si rechaza el pedido (otro 4xx), esRechazoDefinitivo evita repetirlo porque fallaría igual
open class ErrorElevacionException(
    mensaje: String,
    causa: Throwable? = null,
    val esLimitacionDelServicio: Boolean = false,
    val esRechazoDefinitivo: Boolean = false
) : Exception(mensaje, causa)

class SinConexionElevacionException(causa: Throwable?) :
    ErrorElevacionException("Sin conexión para calcular el perfil", causa)
