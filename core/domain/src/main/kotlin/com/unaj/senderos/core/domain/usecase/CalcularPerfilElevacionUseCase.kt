package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.port.FuenteElevacion
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

// Calcula el perfil de elevación del trazado planificado cuando falta o quedó desactualizado, y lo guarda en el sendero
class CalcularPerfilElevacionUseCase @Inject constructor(
    private val senderoRepositorio: SenderoRepositorio,
    private val fuenteElevacion: FuenteElevacion,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Calcular el perfil del sendero indicado del usuario con sesión activa, si falta o quedó desactualizado
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

    // Calcular el perfil de un sendero ya leído; no hace nada si el perfil guardado corresponde al trazado actual
    suspend fun calcularPara(idUsuario: String, sendero: Sendero): Result<Unit> {
        val firma = CalculadoraPerfil.firmaTrazado(sendero.recorrido, sendero.tipoRecorrido)
        if (sendero.perfilElevacion?.firmaTrazado == firma) return Result.success(Unit)
        val muestras = CalculadoraPerfil.muestrearTrazado(sendero.recorrido, sendero.tipoRecorrido)
        if (muestras.isEmpty()) {
            return Result.failure(
                IllegalStateException("El sendero necesita al menos dos puntos de recorrido para calcular el perfil")
            )
        }
        val altitudes = fuenteElevacion.obtenerAltitudes(muestras.map { muestra -> muestra.coordenada })
            .getOrElse { fallo -> return Result.failure(fallo) }
        val perfil = try {
            CalculadoraPerfil.calcular(muestras, altitudes, firma)
        } catch (error: IllegalArgumentException) {
            return Result.failure(error)
        }
        return senderoRepositorio.guardarPerfilElevacion(idUsuario, sendero.id, perfil)
    }
}
