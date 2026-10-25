package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.PuntosCapturadosRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"

// Si era una caminata libre que no se convirtió en sendero, también se borran sus puntos y fotos, porque no tienen
// ningún sendero al que pertenecer
class EliminarRecorridoUseCase @Inject constructor(
    private val recorridoRepositorio: RecorridoRepositorio,
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Primero se elimina el recorrido, que es lo que se pidió. Si después falla el descarte de los puntos, el recorrido
    // igual queda eliminado
    suspend operator fun invoke(idRecorrido: String): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val recorrido = recorridoRepositorio.observarRecorrido(usuario.id, idRecorrido).first() ?: return Result.success(Unit)
        recorridoRepositorio.eliminar(usuario.id, idRecorrido).onFailure { error -> return Result.failure(error) }
        if (IdentificadorCaminataLibre.esDeCaminataLibre(recorrido.idSendero)) {
            val ids = puntosCapturadosRepositorio.observarPendientes(usuario.id, recorrido.idSendero).first().map { punto -> punto.id }
            if (ids.isNotEmpty()) puntosCapturadosRepositorio.descartar(usuario.id, ids)
        }
        return Result.success(Unit)
    }
}
