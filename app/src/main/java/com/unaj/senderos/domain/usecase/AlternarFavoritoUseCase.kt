package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio

class AlternarFavoritoUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Alternar la marca de favorito de un sendero del usuario autenticado
    suspend operator fun invoke(idSendero: String): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))

        return senderoRepositorio.alternarFavorito(usuario.id, idSendero)
    }
}
