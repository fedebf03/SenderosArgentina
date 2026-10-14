package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.database.SenderoDao
import com.unaj.senderos.core.database.PuntoRecorridoEntidad
import com.unaj.senderos.core.database.SenderoEntidad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoSenderoDao(
    entidadesIniciales: List<SenderoEntidad> = emptyList()
) : SenderoDao {

    val entidades: MutableStateFlow<List<SenderoEntidad>> = MutableStateFlow(entidadesIniciales)

    var errorAlGuardar: Throwable? = null

    override fun observarPorUsuario(idUsuario: String): Flow<List<SenderoEntidad>> {
        return entidades.map { listaActual ->
            listaActual
                .filter { entidad -> entidad.idUsuario == idUsuario && !entidad.estaEliminado }
                .sortedBy { entidad -> entidad.nombre }
        }
    }

    override fun observarPorId(idUsuario: String, idSendero: String): Flow<SenderoEntidad?> {
        return entidades.map { listaActual ->
            listaActual.firstOrNull { entidad ->
                entidad.idUsuario == idUsuario && entidad.id == idSendero && !entidad.estaEliminado
            }
        }
    }

    // Obtener un sendero del usuario incluyendo los eliminados
    override suspend fun obtenerPorId(idUsuario: String, idSendero: String): SenderoEntidad? {
        return entidades.value.firstOrNull { entidad -> entidad.idUsuario == idUsuario && entidad.id == idSendero }
    }

    override suspend fun obtenerActivos(idUsuario: String): List<SenderoEntidad> {
        return entidades.value.filter { entidad -> entidad.idUsuario == idUsuario && !entidad.estaEliminado }
    }

    override suspend fun obtenerPendientes(idUsuario: String): List<SenderoEntidad> {
        return entidades.value.filter { entidad ->
            entidad.idUsuario == idUsuario && entidad.estadoSincronizacion != "SINCRONIZADO"
        }
    }

    override suspend fun actualizarEstadoSiNoCambio(
        idUsuario: String,
        idSendero: String,
        fechaLeida: Long,
        estado: String
    ): Int {
        var filasAfectadas = 0
        entidades.update { listaActual ->
            filasAfectadas = 0
            listaActual.map { existente ->
                if (existente.idUsuario == idUsuario && existente.id == idSendero &&
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

    override suspend fun actualizarFotoSiNoCambio(
        idUsuario: String,
        idSendero: String,
        fechaLeida: Long,
        foto: String
    ): Int {
        var filasAfectadas = 0
        entidades.update { listaActual ->
            filasAfectadas = 0
            listaActual.map { existente ->
                if (existente.idUsuario == idUsuario && existente.id == idSendero &&
                    existente.fechaModificacion == fechaLeida
                ) {
                    filasAfectadas++
                    existente.copy(foto = foto)
                } else {
                    existente
                }
            }
        }
        return filasAfectadas
    }

    override suspend fun actualizarRecorridoSiNoCambio(
        idUsuario: String,
        idSendero: String,
        fechaLeida: Long,
        recorrido: List<PuntoRecorridoEntidad>
    ): Int {
        var filasAfectadas = 0
        entidades.update { listaActual ->
            filasAfectadas = 0
            listaActual.map { existente ->
                if (existente.idUsuario == idUsuario && existente.id == idSendero &&
                    existente.fechaModificacion == fechaLeida
                ) {
                    filasAfectadas++
                    existente.copy(recorrido = recorrido)
                } else {
                    existente
                }
            }
        }
        return filasAfectadas
    }

    override suspend fun borrarEliminadoSiNoCambio(idUsuario: String, idSendero: String, fechaLeida: Long): Int {
        var filasAfectadas = 0
        entidades.update { listaActual ->
            val restantes = listaActual.filterNot { existente ->
                existente.idUsuario == idUsuario && existente.id == idSendero &&
                    existente.estaEliminado && existente.fechaModificacion == fechaLeida
            }
            filasAfectadas = listaActual.size - restantes.size
            restantes
        }
        return filasAfectadas
    }

    override suspend fun guardar(entidad: SenderoEntidad) {
        errorAlGuardar?.let { error -> throw error }
        entidades.update { listaActual ->
            listaActual.filterNot { existente ->
                existente.idUsuario == entidad.idUsuario && existente.id == entidad.id
            } + entidad
        }
    }

    override suspend fun borrarDefinitivamente(idUsuario: String, idSendero: String) {
        entidades.update { listaActual ->
            listaActual.filterNot { existente -> existente.idUsuario == idUsuario && existente.id == idSendero }
        }
    }
}
