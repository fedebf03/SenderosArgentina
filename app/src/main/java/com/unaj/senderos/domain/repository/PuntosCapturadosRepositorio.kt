package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.PuntoCapturado
import com.unaj.senderos.domain.model.Sendero
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de los puntos de interés capturados durante un recorrido.
 *
 * Los puntos son locales: no se sincronizan solos, pasan a la nube cuando se incorporan al sendero.
 * Al cerrar sesión se conservan para el mismo usuario: están aislados por `idUsuario` y vuelven a verse
 * cuando ese usuario inicia sesión de nuevo; otro usuario nunca los ve.
 */
interface PuntosCapturadosRepositorio {

    // Guardar el punto capturado en la fuente local, sin depender de la red
    suspend fun guardar(punto: PuntoCapturado): Result<Unit>

    // Observar los puntos capturados de un sendero que todavía no se incorporaron, en orden de captura
    fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturado>>

    // Descartar los puntos capturados con los identificadores indicados y borrar los archivos de sus fotos
    suspend fun descartar(idUsuario: String, ids: Collection<String>): Result<Unit>

    // Incorporar los puntos elegidos al recorrido del sendero y borrarlos de los capturados en una sola transacción (todo o nada).
    // No se cancela con la pantalla. Las fotos de los puntos incorporados pasan al sendero y no se borran
    suspend fun incorporarAlSendero(idUsuario: String, idSendero: String, idsElegidos: Set<String>): Result<Sendero>
}
