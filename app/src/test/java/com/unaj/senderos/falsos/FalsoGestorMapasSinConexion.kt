package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.GestorMapasSinConexion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FalsoGestorMapasSinConexion : GestorMapasSinConexion {
    val estado = MutableStateFlow<EstadoMapaSinConexion>(EstadoMapaSinConexion.NoDescargado)
    var ultimaObservacion: Triple<String, String, String>? = null
        private set
    val descargas = mutableListOf<String>()
    val eliminaciones = mutableListOf<String>()

    // Registrar la observación pedida y devolver el estado configurado
    override fun observar(idUsuario: String, idSendero: String, firmaActual: String): Flow<EstadoMapaSinConexion> {
        ultimaObservacion = Triple(idUsuario, idSendero, firmaActual)
        return estado
    }

    // Registrar la descarga pedida
    override suspend fun descargar(idUsuario: String, sendero: Sendero): Result<Unit> {
        descargas.add("$idUsuario/${sendero.id}")
        return Result.success(Unit)
    }

    // Registrar la eliminación pedida
    override suspend fun eliminar(idUsuario: String, idSendero: String): Result<Unit> {
        eliminaciones.add("$idUsuario/$idSendero")
        return Result.success(Unit)
    }
}
