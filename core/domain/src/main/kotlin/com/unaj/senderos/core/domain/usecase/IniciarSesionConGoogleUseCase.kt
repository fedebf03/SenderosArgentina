package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import javax.inject.Inject

class IniciarSesionConGoogleUseCase @Inject constructor(
    private val repositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(idToken: String): Result<Usuario> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("El token de Google es inválido"))
        }
        return repositorio.iniciarSesionConGoogle(idToken)
    }
}
