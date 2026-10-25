package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.port.GestorMapasSinConexion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FalsoGestorMapasSinConexion : GestorMapasSinConexion {
    val estado = MutableStateFlow<EstadoMapaSinConexion>(EstadoMapaSinConexion.NoDescargado)
    var ultimaObservacion: Triple<String, String, String>? = null
        private set
    val descargas = mutableListOf<String>()
    val eliminaciones = mutableListOf<String>()

    override fun observar(idUsuario: String, idSendero: String, firmaActual: String): Flow<EstadoMapaSinConexion> {
        ultimaObservacion = Triple(idUsuario, idSendero, firmaActual)
        return estado
    }

    override suspend fun descargar(idUsuario: String, sendero: Sendero): Result<Unit> {
        descargas.add("$idUsuario/${sendero.id}")
        return Result.success(Unit)
    }

    override suspend fun eliminar(idUsuario: String, idSendero: String): Result<Unit> {
        eliminaciones.add("$idUsuario/$idSendero")
        return Result.success(Unit)
    }
}
