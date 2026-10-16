package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import javax.inject.Inject

class EnviarCorreoRecuperacionUseCase @Inject constructor(
    private val repositorio: AutenticacionRepositorio
) {
    private val formatoEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("El correo es requerido"))
        if (!formatoEmail.matches(email.trim())) {
            return Result.failure(IllegalArgumentException("El correo no tiene un formato válido"))
        }
        return repositorio.enviarCorreoRecuperacion(email.trim())
    }
}
