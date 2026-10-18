package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.clima.PronosticoClima
import com.unaj.senderos.core.domain.model.Coordenada

interface FuentePronostico {
    // Obtener el clima de ahora y de los próximos días en un lugar; devuelve failure con un mensaje en español si no se puede
    suspend fun obtener(lugar: Coordenada): Result<PronosticoClima>
}

// Error al consultar el clima; el mensaje está pensado para mostrarse al usuario
open class ErrorPronosticoException(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

// No hubo conexión o el servicio no respondió a tiempo
class SinConexionPronosticoException(causa: Throwable?) :
    ErrorPronosticoException("Sin conexión para consultar el clima", causa)
