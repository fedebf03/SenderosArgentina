package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.port.AlmacenBorradorRecorrido
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.PuntosCapturadosRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"
private const val MENSAJE_NADA_PARA_RECUPERAR = "No hay un recorrido para recuperar"

// RF5 - Seguimiento: si Android cerró la app a mitad de un recorrido, se lo ofrecemos al usuario al volver a abrirla
class RecuperarRecorridoInterrumpidoUseCase @Inject constructor(
    private val almacenBorrador: AlmacenBorradorRecorrido,
    private val recorridoRepositorio: RecorridoRepositorio,
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // El borrador de otro usuario ni se ofrece ni se toca
    suspend fun buscar(): RecorridoRealizado? {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual() ?: return null
        return almacenBorrador.leer()?.takeIf { borrador -> borrador.idUsuario == usuario.id }
    }

    // Si no se pudo guardar en el historial, el borrador se conserva para reintentar
    suspend fun recuperar(): Result<RecorridoRealizado> {
        val borrador = buscar() ?: return Result.failure(IllegalStateException(MENSAJE_NADA_PARA_RECUPERAR))
        return recorridoRepositorio.guardar(borrador).map {
            almacenBorrador.borrar()
            borrador
        }
    }

    // En una caminata libre también se descartan los puntos marcados, que no tienen sendero al que pertenecer
    suspend fun descartar(): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val borrador = buscar() ?: return Result.success(Unit)
        if (IdentificadorCaminataLibre.esDeCaminataLibre(borrador.idSendero)) {
            val ids = puntosCapturadosRepositorio.observarPendientes(usuario.id, borrador.idSendero).first().map { punto -> punto.id }
            if (ids.isNotEmpty()) puntosCapturadosRepositorio.descartar(usuario.id, ids).onFailure { error -> return Result.failure(error) }
        }
        almacenBorrador.borrar()
        return Result.success(Unit)
    }
}
