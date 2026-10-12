package com.unaj.senderos.falsos

import com.unaj.senderos.data.local.PuntoCapturadoDao
import com.unaj.senderos.data.local.PuntoCapturadoEntidad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoPuntoCapturadoDao : PuntoCapturadoDao {

    // Tabla local falsa: cualquier cambio se emite a los observadores
    val entidades: MutableStateFlow<List<PuntoCapturadoEntidad>> = MutableStateFlow(emptyList())

    var errorAlGuardar: Throwable? = null
    var errorAlBorrar: Throwable? = null

    // Observar los puntos del sendero ordenados por instante de captura
    override fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturadoEntidad>> {
        return entidades.map { lista -> filtrarPorSendero(lista, idUsuario, idSendero) }
    }

    // Obtener una vez los puntos del sendero ordenados por instante de captura
    override suspend fun obtenerPendientes(idUsuario: String, idSendero: String): List<PuntoCapturadoEntidad> {
        return filtrarPorSendero(entidades.value, idUsuario, idSendero)
    }

    // Obtener una vez los puntos del usuario cuyos identificadores están en la lista
    override suspend fun obtenerPorIds(idUsuario: String, ids: List<String>): List<PuntoCapturadoEntidad> {
        return entidades.value.filter { entidad -> entidad.idUsuario == idUsuario && entidad.id in ids }
    }

    // Insertar o reemplazar la entidad con la misma clave compuesta
    override suspend fun guardar(entidad: PuntoCapturadoEntidad) {
        errorAlGuardar?.let { error -> throw error }
        entidades.update { lista ->
            lista.filterNot { existente -> existente.idUsuario == entidad.idUsuario && existente.id == entidad.id } + entidad
        }
    }

    // Borrar las filas del usuario cuyos identificadores están en la lista, o lanzar el error configurado
    override suspend fun borrar(idUsuario: String, ids: List<String>) {
        errorAlBorrar?.let { error -> throw error }
        entidades.update { lista -> lista.filterNot { entidad -> entidad.idUsuario == idUsuario && entidad.id in ids } }
    }

    // Filtrar las entidades de un sendero del usuario y ordenarlas por instante de captura
    private fun filtrarPorSendero(lista: List<PuntoCapturadoEntidad>, idUsuario: String, idSendero: String): List<PuntoCapturadoEntidad> {
        return lista.filter { entidad -> entidad.idUsuario == idUsuario && entidad.idSendero == idSendero }
            .sortedBy { entidad -> entidad.instanteMilisegundos }
    }
}
