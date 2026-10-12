package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class EnviarCorreoRecuperacionUseCase(
    private val repositorio: AutenticacionRepositorio
) {
    private val formatoEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    // Validar el correo y pedir al repositorio el envío del enlace de recuperación
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("El correo es requerido"))
        if (!formatoEmail.matches(email.trim())) {
            return Result.failure(IllegalArgumentException("El correo no tiene un formato válido"))
        }
        return repositorio.enviarCorreoRecuperacion(email.trim())
    }
}
