package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.SenderosDelUsuario
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservarSenderosDelUsuarioUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    operator fun invoke(): Result<Flow<SenderosDelUsuario>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))

        val flujoSenderos = senderoRepositorio.observarSenderosDelUsuario(usuario.id)
            .map { listaSenderos ->
                SenderosDelUsuario(
                    usuario = usuario,
                    senderos = listaSenderos.filter { sendero -> sendero.idUsuario == usuario.id }
                )
            }
        return Result.success(flujoSenderos)
    }
}
