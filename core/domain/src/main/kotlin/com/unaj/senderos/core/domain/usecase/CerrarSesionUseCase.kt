package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio

class CerrarSesionUseCase(
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val senderoRepositorio: SenderoRepositorio
) {
    operator fun invoke() {
        autenticacionRepositorio.obtenerUsuarioActual()?.let { usuario ->
            senderoRepositorio.detenerSincronizacion(usuario.id)
        }
        autenticacionRepositorio.cerrarSesion()
    }
}
