package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservarDetalleSenderoUseCase @Inject constructor(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    operator fun invoke(idSendero: String): Result<Flow<Sendero?>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))

        val flujoSendero = senderoRepositorio.observarSendero(usuario.id, idSendero)
            .map { sendero -> sendero?.takeIf { senderoEncontrado -> senderoEncontrado.idUsuario == usuario.id } }
        return Result.success(flujoSendero)
    }
}
