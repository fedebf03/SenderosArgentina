package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.port.GestorMapasSinConexion
import com.unaj.senderos.core.domain.recorrido.CalculadoraCorredor
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"

class ObservarMapaSinConexionUseCase @Inject constructor(
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

class DescargarMapaSinConexionUseCase @Inject constructor(
    private val gestorMapasSinConexion: GestorMapasSinConexion,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(sendero: Sendero): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return gestorMapasSinConexion.descargar(usuario.id, sendero)
    }
}

class EliminarMapaSinConexionUseCase @Inject constructor(
    private val gestorMapasSinConexion: GestorMapasSinConexion,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(idSendero: String): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return gestorMapasSinConexion.eliminar(usuario.id, idSendero)
    }
}
