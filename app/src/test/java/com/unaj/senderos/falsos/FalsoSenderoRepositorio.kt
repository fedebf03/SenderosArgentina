package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoSenderoRepositorio(
    senderosIniciales: List<Sendero> = emptyList()
) : SenderoRepositorio {

    // Fuente local falsa: cualquier cambio de este valor se emite a los observadores
    val senderos: MutableStateFlow<List<Sendero>> = MutableStateFlow(senderosIniciales)

    var errorEnFlujo: Throwable? = null
    var resultadoAlternarFavorito: Result<Unit> = Result.success(Unit)
    var resultadoEliminarSendero: Result<Unit> = Result.success(Unit)

    val idsUsuarioConsultados: MutableList<String> = mutableListOf()
    val consultasSendero: MutableList<Pair<String, String>> = mutableListOf()
    val llamadasAlternarFavorito: MutableList<Pair<String, String>> = mutableListOf()
    val llamadasEliminarSendero: MutableList<Pair<String, String>> = mutableListOf()

    // Registrar el usuario consultado y emitir la lista vigente de la fuente falsa
    override fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>> {
        idsUsuarioConsultados.add(idUsuario)
        return flow {
            errorEnFlujo?.let { error -> throw error }
            senderos.collect { listaActual -> emit(listaActual) }
        }
    }

    // Registrar la consulta y emitir el sendero con el identificador pedido o null si no existe
    override fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?> {
        consultasSendero.add(idUsuario to idSendero)
        return flow {
            errorEnFlujo?.let { error -> throw error }
            senderos
                .map { listaActual -> listaActual.firstOrNull { sendero -> sendero.id == idSendero } }
                .collect { senderoEncontrado -> emit(senderoEncontrado) }
        }
    }

    // Registrar la llamada e invertir esFavorito en la fuente falsa si el resultado configurado es exitoso
    override suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit> {
        llamadasAlternarFavorito.add(idUsuario to idSendero)
        if (resultadoAlternarFavorito.isSuccess) {
            senderos.update { listaActual ->
                listaActual.map { sendero ->
                    if (sendero.id == idSendero) sendero.copy(esFavorito = !sendero.esFavorito) else sendero
                }
            }
        }
        return resultadoAlternarFavorito
    }

    // Registrar la llamada y quitar el sendero de la fuente falsa si el resultado configurado es exitoso
    override suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit> {
        llamadasEliminarSendero.add(idUsuario to idSendero)
        if (resultadoEliminarSendero.isSuccess) {
            senderos.update { listaActual -> listaActual.filter { sendero -> sendero.id != idSendero } }
        }
        return resultadoEliminarSendero
    }
}
