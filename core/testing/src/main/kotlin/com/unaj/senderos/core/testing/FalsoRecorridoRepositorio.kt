package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.domain.repository.RecorridoRepositorio
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

    override suspend fun guardar(recorrido: RecorridoRealizado): Result<Unit> {
        guardados.add(recorrido)
        errorAlGuardar?.let { error -> throw error }
        return resultadoGuardar
    }
}
