package com.unaj.senderos.falsos

import com.unaj.senderos.data.local.RecorridoDao
import com.unaj.senderos.data.local.RecorridoEntidad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoRecorridoDao(
    entidadesIniciales: List<RecorridoEntidad> = emptyList()
) : RecorridoDao {

    val entidades: MutableStateFlow<List<RecorridoEntidad>> = MutableStateFlow(entidadesIniciales)

    var errorAlGuardar: Throwable? = null

    override fun observarPorUsuario(idUsuario: String): Flow<List<RecorridoEntidad>> {
        return entidades.map { lista ->
            lista.filter { entidad -> entidad.idUsuario == idUsuario }
                .sortedByDescending { entidad -> entidad.inicioMilisegundos }
        }
    }

    override fun observarPorId(idUsuario: String, idRecorrido: String): Flow<RecorridoEntidad?> {
        return entidades.map { lista ->
            lista.firstOrNull { entidad -> entidad.idUsuario == idUsuario && entidad.id == idRecorrido }
        }
    }

    override suspend fun obtenerPorId(idUsuario: String, idRecorrido: String): RecorridoEntidad? {
        return entidades.value.firstOrNull { entidad -> entidad.idUsuario == idUsuario && entidad.id == idRecorrido }
    }

    override suspend fun obtenerPendientes(idUsuario: String): List<RecorridoEntidad> {
        return entidades.value.filter { entidad ->
            entidad.idUsuario == idUsuario && entidad.estadoSincronizacion != "SINCRONIZADO"
        }
    }

    override suspend fun guardar(entidad: RecorridoEntidad) {
        errorAlGuardar?.let { error -> throw error }
        entidades.update { lista ->
            lista.filterNot { existente -> existente.idUsuario == entidad.idUsuario && existente.id == entidad.id } + entidad
        }
    }

    override suspend fun actualizarEstadoSiNoCambio(
        idUsuario: String,
        idRecorrido: String,
        fechaLeida: Long,
        estado: String
    ): Int {
        var filasAfectadas = 0
        entidades.update { lista ->
            filasAfectadas = 0
            lista.map { existente ->
                if (existente.idUsuario == idUsuario && existente.id == idRecorrido &&
                    existente.fechaModificacion == fechaLeida
                ) {
                    filasAfectadas++
                    existente.copy(estadoSincronizacion = estado)
                } else {
                    existente
                }
            }
        }
        return filasAfectadas
    }
}
