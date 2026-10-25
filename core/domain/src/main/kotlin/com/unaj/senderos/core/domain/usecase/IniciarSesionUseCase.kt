package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import javax.inject.Inject

class IniciarSesionUseCase @Inject constructor(
    private val repositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(email: String, clave: String): Result<Usuario> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("El correo es requerido"))
        if (clave.isBlank()) return Result.failure(IllegalArgumentException("La contraseña es requerida"))
        return repositorio.iniciarSesion(email.trim(), clave)
    }
}
