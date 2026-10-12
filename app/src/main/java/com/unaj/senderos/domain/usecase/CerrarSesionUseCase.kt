package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.LanzadorSeguimiento
import com.unaj.senderos.domain.repository.SenderoRepositorio

class CerrarSesionUseCase(
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val senderoRepositorio: SenderoRepositorio,
    private val lanzadorSeguimiento: LanzadorSeguimiento
) {
    // Cerrar la sesión deteniendo antes el seguimiento y la sincronización del usuario
    operator fun invoke() {
        lanzadorSeguimiento.detener()
        autenticacionRepositorio.obtenerUsuarioActual()?.let { usuario ->
            senderoRepositorio.detenerSincronizacion(usuario.id)
        }
        autenticacionRepositorio.cerrarSesion()
    }
}
