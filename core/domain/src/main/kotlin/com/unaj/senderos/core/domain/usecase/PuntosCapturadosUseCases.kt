package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.CalculadoraInsercionPuntos
import com.unaj.senderos.domain.model.PuntoCapturado
import com.unaj.senderos.domain.model.PuntoCapturadoConDistancia
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.PuntosCapturadosRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"

class ObservarPuntosCapturadosUseCase(
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Observar los puntos capturados pendientes de un sendero del usuario con sesión activa, en orden de captura
    operator fun invoke(idSendero: String): Result<Flow<List<PuntoCapturado>>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return Result.success(puntosCapturadosRepositorio.observarPendientes(usuario.id, idSendero))
    }
}

class ObservarPuntosCapturadosConDistanciaUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Observar los puntos capturados de un sendero junto con su distancia al trazado, para que la UI advierta y desmarque los de más de 100 m
    operator fun invoke(idSendero: String): Result<Flow<List<PuntoCapturadoConDistancia>>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val flujo = combine(
            senderoRepositorio.observarSendero(usuario.id, idSendero),
            puntosCapturadosRepositorio.observarPendientes(usuario.id, idSendero)
        ) { sendero, puntos -> asociarDistancias(sendero, puntos) }
        return Result.success(flujo)
    }

    // Asociar a cada punto su distancia al trazado del sendero; sin sendero o sin recorrido la distancia queda desconocida
    private fun asociarDistancias(sendero: Sendero?, puntos: List<PuntoCapturado>): List<PuntoCapturadoConDistancia> {
        val distancias = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(
            sendero?.recorrido.orEmpty(),
            sendero?.tipoRecorrido ?: TipoRecorrido.LINEAL,
            puntos.map { punto -> PuntoRecorrido(punto.latitud, punto.longitud) }
        )
        return puntos.mapIndexed { indice, punto -> PuntoCapturadoConDistancia(punto, distancias[indice]) }
    }
}

class DescartarPuntosCapturadosUseCase(
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Descartar los puntos capturados indicados del usuario con sesión activa
    suspend operator fun invoke(ids: Set<String>): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return puntosCapturadosRepositorio.descartar(usuario.id, ids)
    }
}

class IncorporarPuntosCapturadosUseCase(
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Incorporar los puntos elegidos al sendero del usuario con sesión activa de forma atómica; el sendero queda pendiente de sincronizar
    suspend operator fun invoke(idSendero: String, idsElegidos: Set<String>): Result<Sendero> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return puntosCapturadosRepositorio.incorporarAlSendero(usuario.id, idSendero, idsElegidos)
    }
}
