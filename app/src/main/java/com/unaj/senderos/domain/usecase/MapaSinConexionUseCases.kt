package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.port.GestorMapasSinConexion
import com.unaj.senderos.domain.recorrido.CalculadoraCorredor
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import kotlinx.coroutines.flow.Flow

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"

class ObservarMapaSinConexionUseCase(
    private val gestorMapasSinConexion: GestorMapasSinConexion,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    operator fun invoke(sendero: Sendero): Result<Flow<EstadoMapaSinConexion>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val firmaActual = CalculadoraCorredor.firmaTrazado(sendero.recorrido)
        return Result.success(gestorMapasSinConexion.observar(usuario.id, sendero.id, firmaActual))
    }
}

class DescargarMapaSinConexionUseCase(
    private val gestorMapasSinConexion: GestorMapasSinConexion,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(sendero: Sendero): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return gestorMapasSinConexion.descargar(usuario.id, sendero)
    }
}

class EliminarMapaSinConexionUseCase(
    private val gestorMapasSinConexion: GestorMapasSinConexion,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(idSendero: String): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return gestorMapasSinConexion.eliminar(usuario.id, idSendero)
    }
}
