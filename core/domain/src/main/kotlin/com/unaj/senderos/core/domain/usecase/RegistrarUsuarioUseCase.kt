package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import javax.inject.Inject

class RegistrarUsuarioUseCase @Inject constructor(
    private val repositorio: AutenticacionRepositorio
) {
    private val formatoEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    suspend operator fun invoke(email: String, clave: String, confirmacion: String): Result<Usuario> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("El correo es requerido"))
        if (!formatoEmail.matches(email.trim())) {
            return Result.failure(IllegalArgumentException("El correo no tiene un formato válido"))
        }
        if (clave.length < 6) {
            return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres"))
        }
        if (clave != confirmacion) {
            return Result.failure(IllegalArgumentException("Las contraseñas no coinciden"))
        }
        return repositorio.registrar(email.trim(), clave)
    }
}
