package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoSenderoRepositorio(
    senderosIniciales: List<Sendero> = emptyList()
) : SenderoRepositorio {

    val senderos: MutableStateFlow<List<Sendero>> = MutableStateFlow(senderosIniciales)

    var errorEnFlujo: Throwable? = null
    var resultadoAlternarFavorito: Result<Unit> = Result.success(Unit)
    var resultadoEliminarSendero: Result<Unit> = Result.success(Unit)
    var resultadoGuardarSendero: Result<Unit> = Result.success(Unit)
    var resultadoGuardarPerfil: Result<Unit> = Result.success(Unit)

    val idsUsuarioConsultados: MutableList<String> = mutableListOf()
    val consultasSendero: MutableList<Pair<String, String>> = mutableListOf()
    val llamadasAlternarFavorito: MutableList<Pair<String, String>> = mutableListOf()
    val llamadasEliminarSendero: MutableList<Pair<String, String>> = mutableListOf()
    val llamadasGuardarSendero: MutableList<Pair<String, Sendero>> = mutableListOf()
    val llamadasGuardarPerfil: MutableList<Triple<String, String, PerfilElevacion>> = mutableListOf()

    val idsUsuarioDetenidos: MutableList<String> = mutableListOf()
    var alDetenerSincronizacion: ((String) -> Unit)? = null

    override fun detenerSincronizacion(idUsuario: String) {
        idsUsuarioDetenidos.add(idUsuario)
        alDetenerSincronizacion?.invoke(idUsuario)
    }

    override fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>> {
        idsUsuarioConsultados.add(idUsuario)
        return flow {
            errorEnFlujo?.let { error -> throw error }
            senderos.collect { listaActual -> emit(listaActual) }
        }
    }

    override fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?> {
        consultasSendero.add(idUsuario to idSendero)
        return flow {
            errorEnFlujo?.let { error -> throw error }
            senderos
                .map { listaActual -> listaActual.firstOrNull { sendero -> sendero.id == idSendero } }
                .collect { senderoEncontrado -> emit(senderoEncontrado) }
        }
    }

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

    override suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit> {
        llamadasEliminarSendero.add(idUsuario to idSendero)
        if (resultadoEliminarSendero.isSuccess) {
            senderos.update { listaActual -> listaActual.filter { sendero -> sendero.id != idSendero } }
        }
        return resultadoEliminarSendero
    }

    override suspend fun guardarSendero(idUsuario: String, sendero: Sendero): Result<Unit> {
        llamadasGuardarSendero.add(idUsuario to sendero)
        if (resultadoGuardarSendero.isSuccess) {
            senderos.update { listaActual ->
                if (listaActual.any { existente -> existente.id == sendero.id }) {
                    listaActual.map { existente -> if (existente.id == sendero.id) sendero else existente }
                } else {
                    listaActual + sendero
                }
            }
        }
        return resultadoGuardarSendero
    }

    override suspend fun guardarPerfilElevacion(idUsuario: String, idSendero: String, perfil: PerfilElevacion): Result<Unit> {
        llamadasGuardarPerfil.add(Triple(idUsuario, idSendero, perfil))
        if (resultadoGuardarPerfil.isSuccess) {
            senderos.update { listaActual ->
                listaActual.map { sendero ->
                    if (sendero.id == idSendero) CalculadoraPerfil.aplicarAlSendero(sendero, perfil) else sendero
                }
            }
        }
        return resultadoGuardarPerfil
    }
}
