package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.port.FuenteElevacion
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

// RF3 - Detalle: calculamos el perfil de elevación cuando falta o quedó desactualizado y lo guardamos en el sendero
class CalcularPerfilElevacionUseCase @Inject constructor(
    private val senderoRepositorio: SenderoRepositorio,
    private val fuenteElevacion: FuenteElevacion,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    private val calcularPerfilDeTrazado = CalcularPerfilDeTrazadoUseCase(fuenteElevacion)

    suspend operator fun invoke(idSendero: String): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))
        val sendero = try {
            senderoRepositorio.observarSendero(usuario.id, idSendero).first()
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            return Result.failure(error)
        } ?: return Result.failure(NoSuchElementException("No se encontró el sendero"))
        return calcularPara(usuario.id, sendero)
    }

    // No hace nada si el perfil guardado corresponde al trazado actual
    suspend fun calcularPara(idUsuario: String, sendero: Sendero): Result<Unit> {
        val firma = CalculadoraPerfil.firmaTrazado(sendero.recorrido, sendero.tipoRecorrido)
        if (sendero.perfilElevacion?.firmaTrazado == firma) return Result.success(Unit)
        val perfil = calcularPerfilDeTrazado(sendero.recorrido, sendero.tipoRecorrido)
            .getOrElse { fallo -> return Result.failure(fallo) }
        return senderoRepositorio.guardarPerfilElevacion(idUsuario, sendero.id, perfil)
    }
}
