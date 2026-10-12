package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.RecorridoRepositorio
import kotlinx.coroutines.flow.Flow

class ObservarRecorridoUseCase(
    private val recorridoRepositorio: RecorridoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Observar un recorrido del usuario con sesión activa; el flujo emite null si no existe
    operator fun invoke(idRecorrido: String): Result<Flow<RecorridoRealizado?>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))
        return Result.success(recorridoRepositorio.observarRecorrido(usuario.id, idRecorrido))
    }
}
