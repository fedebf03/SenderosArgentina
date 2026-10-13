package com.unaj.senderos.domain.port

import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.Sendero
import kotlinx.coroutines.flow.Flow

interface GestorMapasSinConexion {
    fun observar(idUsuario: String, idSendero: String, firmaActual: String): Flow<EstadoMapaSinConexion>

    // Iniciar la descarga del corredor del sendero; devuelve al arrancar y el avance se sigue con observar
    suspend fun descargar(idUsuario: String, sendero: Sendero): Result<Unit>

    // Eliminar el mapa descargado del sendero; no falla si no existía
    suspend fun eliminar(idUsuario: String, idSendero: String): Result<Unit>
}
