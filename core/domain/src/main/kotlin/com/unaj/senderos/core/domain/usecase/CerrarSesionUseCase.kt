package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.port.LanzadorSeguimiento
import com.unaj.senderos.core.domain.port.Notificador
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import javax.inject.Inject

class CerrarSesionUseCase @Inject constructor(
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
