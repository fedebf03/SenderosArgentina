package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.clima.PronosticoClima
import com.unaj.senderos.core.domain.model.Coordenada

interface FuentePronostico {
    suspend fun obtener(lugar: Coordenada): Result<PronosticoClima>
}

// El mensaje está pensado para mostrarse al usuario
open class ErrorPronosticoException(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

class SinConexionPronosticoException(causa: Throwable?) :
    ErrorPronosticoException("Sin conexión para consultar el clima", causa)
