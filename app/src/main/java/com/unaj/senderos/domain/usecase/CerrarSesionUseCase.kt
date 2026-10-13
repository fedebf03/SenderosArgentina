package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.port.LanzadorSeguimiento
import com.unaj.senderos.domain.port.Notificador
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio

class CerrarSesionUseCase(
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val senderoRepositorio: SenderoRepositorio,
    private val lanzadorSeguimiento: LanzadorSeguimiento,
    private val notificador: Notificador
) {
    operator fun invoke() {
        lanzadorSeguimiento.detener()
        autenticacionRepositorio.obtenerUsuarioActual()?.let { usuario ->
            senderoRepositorio.detenerSincronizacion(usuario.id)
        }
        notificador.cancelarAvisoDeError()
        autenticacionRepositorio.cerrarSesion()
    }
}
