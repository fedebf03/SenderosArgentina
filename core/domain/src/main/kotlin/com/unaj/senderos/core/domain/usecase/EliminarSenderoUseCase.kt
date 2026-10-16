package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import javax.inject.Inject

class EliminarSenderoUseCase @Inject constructor(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(idSendero: String): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))

        return senderoRepositorio.eliminarSendero(usuario.id, idSendero)
    }
}
