package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada

interface FuenteElevacion {
    // Obtener la altitud en metros de cada coordenada, en el mismo orden; devuelve failure con un mensaje en español si no se puede
    suspend fun obtenerAltitudes(coordenadas: List<Coordenada>): Result<List<Double>>
}

// Error al consultar las alturas del terreno; el mensaje está pensado para mostrarse al usuario.
// esLimitacionDelServicio indica un límite o una caída del servicio (429, 5xx): insistir con más pedidos no sirve.
// esRechazoDefinitivo indica que el servicio rechazó este pedido (4xx distinto de 429): repetirlo igual dará el mismo error
open class ErrorElevacionException(
    mensaje: String,
    causa: Throwable? = null,
    val esLimitacionDelServicio: Boolean = false,
    val esRechazoDefinitivo: Boolean = false
) : Exception(mensaje, causa)

// No hubo conexión o el servicio no respondió a tiempo
class SinConexionElevacionException(causa: Throwable?) :
    ErrorElevacionException("Sin conexión para calcular el perfil", causa)
