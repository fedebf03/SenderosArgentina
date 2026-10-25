package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import javax.inject.Inject

class ObtenerUsuarioActualUseCase @Inject constructor(
    private val repositorio: AutenticacionRepositorio
) {
    operator fun invoke(): Usuario? {
        return repositorio.obtenerUsuarioActual()
    }
}
