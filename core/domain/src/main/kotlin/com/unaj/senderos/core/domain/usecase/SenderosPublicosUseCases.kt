package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.SenderoPublico
import com.unaj.senderos.core.domain.model.esFotoLocal
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoPublicoRepositorio
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"

class ObservarSenderosPublicosUseCase @Inject constructor(
    private val senderoPublicoRepositorio: SenderoPublicoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    operator fun invoke(): Result<Flow<List<SenderoPublico>>> {
        autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return Result.success(senderoPublicoRepositorio.observarSenderosPublicos())
    }
}

class ObservarSenderoPublicoUseCase @Inject constructor(
    private val senderoPublicoRepositorio: SenderoPublicoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    operator fun invoke(idSendero: String): Result<Flow<SenderoPublico?>> {
        autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return Result.success(senderoPublicoRepositorio.observarSenderoPublico(idSendero))
    }
}

class ObservarSenderoPublicadoUseCase @Inject constructor(
    private val senderoPublicoRepositorio: SenderoPublicoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    operator fun invoke(idSendero: String): Result<Flow<Boolean>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val flujo = senderoPublicoRepositorio.observarSenderoPublico(idSendero)
            .map { publico -> publico != null && publico.idAutor == usuario.id }
        return Result.success(flujo)
    }
}

class PublicarSenderoUseCase @Inject constructor(
    private val senderoPublicoRepositorio: SenderoPublicoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(sendero: Sendero): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        if (sendero.idUsuario != usuario.id || sendero.idOrigen.isNotEmpty()) {
            return Result.failure(IllegalStateException("Solo podés publicar tus propios senderos"))
        }
        if (!estaListoParaPublicar(sendero)) {
            return Result.failure(
                IllegalStateException("Esperá a que el sendero termine de sincronizarse para publicarlo")
            )
        }
        val nombreAutor = usuario.nombre.ifBlank { usuario.email.substringBefore("@") }
        return senderoPublicoRepositorio.publicar(
            SenderoPublico(sendero = sendero.copy(esFavorito = false), nombreAutor = nombreAutor)
        )
    }

    private fun estaListoParaPublicar(sendero: Sendero): Boolean {
        val hayFotoLocal = esFotoLocal(sendero.foto) || sendero.recorrido.any { punto -> esFotoLocal(punto.foto) }
        return sendero.estadoSincronizacion == EstadoSincronizacion.SINCRONIZADO && !hayFotoLocal
    }
}

class DespublicarSenderoUseCase @Inject constructor(
    private val senderoPublicoRepositorio: SenderoPublicoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    suspend operator fun invoke(idSendero: String): Result<Unit> {
        autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return senderoPublicoRepositorio.despublicar(idSendero)
    }
}

class GuardarCopiaSenderoPublicoUseCase @Inject constructor(
    private val guardarSenderoUseCase: GuardarSenderoUseCase
) {
    suspend operator fun invoke(publico: SenderoPublico): Result<Sendero> {
        val copia = publico.sendero.copy(
            id = "",
            idUsuario = "",
            esFavorito = false,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE,
            idOrigen = publico.id
        )
        return guardarSenderoUseCase(copia)
    }
}
