package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class IniciarSesionConGoogleUseCase(
    private val repositorio: AutenticacionRepositorio
) {
    // Ejecutar la autenticación con el token de ID de Google
    suspend operator fun invoke(idToken: String): Result<Usuario> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("El token de Google es inválido"))
        }
        return repositorio.iniciarSesionConGoogle(idToken)
    }
}
