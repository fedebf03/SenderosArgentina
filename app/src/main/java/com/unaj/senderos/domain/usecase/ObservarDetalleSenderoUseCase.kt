package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservarDetalleSenderoUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Observar un sendero del usuario autenticado, emitiendo null si no le pertenece
    operator fun invoke(idSendero: String): Result<Flow<Sendero?>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))

        val flujoSendero = senderoRepositorio.observarSendero(usuario.id, idSendero)
            .map { sendero -> sendero?.takeIf { senderoEncontrado -> senderoEncontrado.idUsuario == usuario.id } }
        return Result.success(flujoSendero)
    }
}
