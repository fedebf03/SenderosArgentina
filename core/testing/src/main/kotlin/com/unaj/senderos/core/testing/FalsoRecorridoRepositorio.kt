package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FalsoRecorridoRepositorio : RecorridoRepositorio {

    val recorridos: MutableStateFlow<List<RecorridoRealizado>> = MutableStateFlow(emptyList())

    var resultadoGuardar: Result<Unit> = Result.success(Unit)
    var errorAlGuardar: Throwable? = null

    val guardados: MutableList<RecorridoRealizado> = mutableListOf()
    val idsUsuarioConsultados: MutableList<String> = mutableListOf()

    override fun observarRecorridos(idUsuario: String): Flow<List<RecorridoRealizado>> {
        idsUsuarioConsultados.add(idUsuario)
        return recorridos.map { lista ->
            lista.filter { recorrido -> recorrido.idUsuario == idUsuario }
                .sortedByDescending { recorrido -> recorrido.inicioMilisegundos }
        }
    }

    override fun observarRecorrido(idUsuario: String, idRecorrido: String): Flow<RecorridoRealizado?> {
        idsUsuarioConsultados.add(idUsuario)
        return recorridos.map { lista ->
            lista.firstOrNull { recorrido -> recorrido.idUsuario == idUsuario && recorrido.id == idRecorrido }
        }
    }

    val eliminados: MutableList<Pair<String, String>> = mutableListOf()
    var resultadoEliminar: Result<Unit> = Result.success(Unit)

    override suspend fun eliminar(idUsuario: String, idRecorrido: String): Result<Unit> {
        eliminados.add(idUsuario to idRecorrido)
        if (resultadoEliminar.isSuccess) {
            recorridos.value = recorridos.value.filterNot { recorrido -> recorrido.idUsuario == idUsuario && recorrido.id == idRecorrido }
        }
        return resultadoEliminar
    }

    override suspend fun guardar(recorrido: RecorridoRealizado): Result<Unit> {
        guardados.add(recorrido)
        errorAlGuardar?.let { error -> throw error }
        return resultadoGuardar
    }
}
