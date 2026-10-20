package com.unaj.senderos.core.domain.repository

import com.unaj.senderos.core.domain.model.PuntoCapturado
import com.unaj.senderos.core.domain.model.Sendero
import kotlinx.coroutines.flow.Flow

// Los puntos no se sincronizan solos y pasan a la nube cuando se incorporan al sendero. Al cerrar sesión
// se conservan aislados por idUsuario, así vuelven a verse con el mismo usuario y otro nunca los ve
interface PuntosCapturadosRepositorio {

    suspend fun guardar(punto: PuntoCapturado): Result<Unit>

    // Solo los que todavía no se incorporaron, en orden de captura
    fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturado>>

    // Borra también los archivos de las fotos
    suspend fun descartar(idUsuario: String, ids: Collection<String>): Result<Unit>

    // Se conservan los archivos de las fotos porque ahora los usa el sendero que nació de la caminata
    suspend fun retirarIncorporados(idUsuario: String, ids: Collection<String>): Result<Unit>
    // Ocurre en una sola transacción y no se cancela con la pantalla. Las fotos pasan al sendero y no se borran
    suspend fun incorporarAlSendero(idUsuario: String, idSendero: String, idsElegidos: Set<String>): Result<Sendero>
}
