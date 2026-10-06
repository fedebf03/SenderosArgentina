package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class CerrarSesionUseCase(
    private val repositorio: AutenticacionRepositorio
) {
    // Cerrar la sesión del usuario actual en el repositorio
    operator fun invoke() {
        repositorio.cerrarSesion()
    }
}
