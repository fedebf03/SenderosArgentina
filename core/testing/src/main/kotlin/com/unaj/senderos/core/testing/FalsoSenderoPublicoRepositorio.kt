package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoPublico
import com.unaj.senderos.core.domain.repository.SenderoPublicoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoSenderoPublicoRepositorio(
    publicosIniciales: List<SenderoPublico> = emptyList()
) : SenderoPublicoRepositorio {

    val publicos: MutableStateFlow<List<SenderoPublico>> = MutableStateFlow(publicosIniciales)

    var errorEnFlujo: Throwable? = null
    var resultadoPublicar: Result<Unit> = Result.success(Unit)
    var resultadoDespublicar: Result<Unit> = Result.success(Unit)

    val llamadasPublicar: MutableList<SenderoPublico> = mutableListOf()
    val llamadasDespublicar: MutableList<String> = mutableListOf()

    override fun observarSenderosPublicos(): Flow<List<SenderoPublico>> {
        return flow {
            errorEnFlujo?.let { error -> throw error }
            publicos.collect { listaActual -> emit(listaActual) }
        }
    }

    override fun observarSenderoPublico(idSendero: String): Flow<SenderoPublico?> {
        return flow {
            errorEnFlujo?.let { error -> throw error }
            publicos
                .map { listaActual -> listaActual.firstOrNull { publico -> publico.id == idSendero } }
                .collect { encontrado -> emit(encontrado) }
        }
    }

    override suspend fun publicar(sendero: SenderoPublico): Result<Unit> {
        llamadasPublicar.add(sendero)
        if (resultadoPublicar.isSuccess) {
            publicos.update { listaActual -> listaActual.filter { publico -> publico.id != sendero.id } + sendero }
        }
        return resultadoPublicar
    }

    override suspend fun despublicar(idSendero: String): Result<Unit> {
        llamadasDespublicar.add(idSendero)
        if (resultadoDespublicar.isSuccess) {
            publicos.update { listaActual -> listaActual.filter { publico -> publico.id != idSendero } }
        }
        return resultadoDespublicar
    }
}

fun crearSenderoPublicoDePrueba(
    sendero: Sendero = crearSenderoDePrueba(id = "publico-1", idUsuario = "autor-1"),
    nombreAutor: String = "Luis",
    fechaPublicacion: Long = 1_000L
): SenderoPublico {
    return SenderoPublico(sendero = sendero, nombreAutor = nombreAutor, fechaPublicacion = fechaPublicacion)
}
