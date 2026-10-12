package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class IniciarSesionUseCase(
    private val repositorio: AutenticacionRepositorio
) {
    // Ejecutar el caso de uso
    suspend operator fun invoke(email: String, clave: String): Result<Usuario> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("El correo es requerido"))
        if (clave.isBlank()) return Result.failure(IllegalArgumentException("La contraseña es requerida"))
        return repositorio.iniciarSesion(email.trim(), clave)
    }
}
