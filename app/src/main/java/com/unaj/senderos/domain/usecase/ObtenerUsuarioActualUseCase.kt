package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio

class ObtenerUsuarioActualUseCase(
    private val repositorio: AutenticacionRepositorio
) {
    operator fun invoke(): Usuario? {
        return repositorio.obtenerUsuarioActual()
    }
}
