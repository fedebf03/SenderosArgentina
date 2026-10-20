package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.database.puntocapturado.PuntoCapturadoDao
import com.unaj.senderos.core.database.puntocapturado.PuntoCapturadoEntidad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoPuntoCapturadoDao : PuntoCapturadoDao {

    val entidades: MutableStateFlow<List<PuntoCapturadoEntidad>> = MutableStateFlow(emptyList())

    var errorAlGuardar: Throwable? = null
    var errorAlBorrar: Throwable? = null

    override fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturadoEntidad>> {
        return entidades.map { lista -> filtrarPorSendero(lista, idUsuario, idSendero) }
    }

    override suspend fun obtenerPendientes(idUsuario: String, idSendero: String): List<PuntoCapturadoEntidad> {
        return filtrarPorSendero(entidades.value, idUsuario, idSendero)
    }

    override suspend fun obtenerPorIds(idUsuario: String, ids: List<String>): List<PuntoCapturadoEntidad> {
        return entidades.value.filter { entidad -> entidad.idUsuario == idUsuario && entidad.id in ids }
    }

    override suspend fun guardar(entidad: PuntoCapturadoEntidad) {
        errorAlGuardar?.let { error -> throw error }
        entidades.update { lista ->
            lista.filterNot { existente -> existente.idUsuario == entidad.idUsuario && existente.id == entidad.id } + entidad
        }
    }

    override suspend fun borrar(idUsuario: String, ids: List<String>) {
        errorAlBorrar?.let { error -> throw error }
        entidades.update { lista -> lista.filterNot { entidad -> entidad.idUsuario == idUsuario && entidad.id in ids } }
    }

    private fun filtrarPorSendero(lista: List<PuntoCapturadoEntidad>, idUsuario: String, idSendero: String): List<PuntoCapturadoEntidad> {
        return lista.filter { entidad -> entidad.idUsuario == idUsuario && entidad.idSendero == idSendero }
            .sortedBy { entidad -> entidad.instanteMilisegundos }
    }
}
