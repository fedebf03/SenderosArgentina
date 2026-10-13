package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.PuntoCapturado
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.recorrido.CalculadoraInsercionPuntos
import com.unaj.senderos.domain.repository.PuntosCapturadosRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FalsoPuntosCapturadosRepositorio(
    puntosIniciales: List<PuntoCapturado> = emptyList()
) : PuntosCapturadosRepositorio {

    // Tabla local falsa: cualquier cambio se emite a los observadores
    val puntos: MutableStateFlow<List<PuntoCapturado>> = MutableStateFlow(puntosIniciales)

    var resultadoGuardar: Result<Unit> = Result.success(Unit)
    var resultadoDescartar: Result<Unit> = Result.success(Unit)
    var resultadoIncorporar: Result<Sendero>? = null

    // Repositorio de senderos falso opcional: si está, incorporar emula la inserción real y guarda el sendero en él
    var senderoRepositorio: FalsoSenderoRepositorio? = null
    val llamadasGuardar: MutableList<PuntoCapturado> = mutableListOf()
    val llamadasDescartar: MutableList<Pair<String, Collection<String>>> = mutableListOf()
    val llamadasIncorporar: MutableList<Triple<String, String, Set<String>>> = mutableListOf()

    // Registrar la llamada y agregar el punto si el resultado configurado es exitoso
    override suspend fun guardar(punto: PuntoCapturado): Result<Unit> {
        llamadasGuardar.add(punto)
        if (resultadoGuardar.isSuccess) {
            puntos.update { lista -> lista.filterNot { existente -> existente.id == punto.id } + punto }
        }
        return resultadoGuardar
    }

    // Observar los puntos del sendero ordenados por instante de captura
    override fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturado>> {
        return puntos.map { lista ->
            lista.filter { punto -> punto.idUsuario == idUsuario && punto.idSendero == idSendero }
                .sortedBy { punto -> punto.instanteMilisegundos }
        }
    }

    // Registrar la llamada y quitar los puntos si el resultado configurado es exitoso
    override suspend fun descartar(idUsuario: String, ids: Collection<String>): Result<Unit> {
        llamadasDescartar.add(idUsuario to ids)
        if (resultadoDescartar.isSuccess) {
            puntos.update { lista -> lista.filterNot { punto -> punto.idUsuario == idUsuario && punto.id in ids } }
        }
        return resultadoDescartar
    }

    // Registrar la llamada y devolver el resultado configurado
    override suspend fun incorporarAlSendero(idUsuario: String, idSendero: String, idsElegidos: Set<String>): Result<Sendero> {
        llamadasIncorporar.add(Triple(idUsuario, idSendero, idsElegidos))
        resultadoIncorporar?.let { configurado -> return configurado }
        val repositorioDeSenderos = senderoRepositorio ?: return Result.failure(IllegalStateException("Sin configurar"))
        val sendero = repositorioDeSenderos.observarSendero(idUsuario, idSendero).first()
            ?: return Result.failure(IllegalStateException("No se encontró el sendero"))
        val elegidos = puntos.value.filter { punto -> punto.idUsuario == idUsuario && punto.idSendero == idSendero && punto.id in idsElegidos }
            .sortedBy { punto -> punto.instanteMilisegundos }
        if (elegidos.isEmpty()) return Result.success(sendero)
        val recorridoNuevo = CalculadoraInsercionPuntos.insertar(
            sendero.recorrido,
            sendero.tipoRecorrido,
            elegidos.map { punto -> PuntoRecorrido(punto.latitud, punto.longitud, punto.tipo, punto.nota.trim(), punto.foto) }
        )
        val actualizado = sendero.copy(recorrido = recorridoNuevo)
        repositorioDeSenderos.guardarSendero(idUsuario, actualizado).onFailure { error -> return Result.failure(error) }
        puntos.update { lista -> lista.filterNot { punto -> punto in elegidos } }
        return Result.success(actualizado)
    }
}
