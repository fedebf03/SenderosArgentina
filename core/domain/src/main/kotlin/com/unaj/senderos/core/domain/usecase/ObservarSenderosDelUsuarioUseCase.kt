package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.SenderosDelUsuario
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservarSenderosDelUsuarioUseCase @Inject constructor(
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
