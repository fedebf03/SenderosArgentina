package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.domain.model.Sendero
import kotlinx.coroutines.flow.Flow

interface GestorMapasSinConexion {
    fun observar(idUsuario: String, idSendero: String, firmaActual: String): Flow<EstadoMapaSinConexion>

    // Vuelve apenas arranca la descarga y el avance se sigue con observar
    suspend fun descargar(idUsuario: String, sendero: Sendero): Result<Unit>

    suspend fun eliminar(idUsuario: String, idSendero: String): Result<Unit>
}
