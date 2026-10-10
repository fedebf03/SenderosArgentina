package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.SenderosDelUsuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio

class ObtenerSenderosDelUsuarioUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Obtener el usuario autenticado junto con sus senderos, descartando los de otros usuarios
    suspend operator fun invoke(): Result<SenderosDelUsuario> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))

        return senderoRepositorio.obtenerSenderosDelUsuario(usuario.id).map { listaSenderos ->
            SenderosDelUsuario(
                usuario = usuario,
                senderos = listaSenderos.filter { sendero -> sendero.idUsuario == usuario.id }
            )
        }
    }
}
