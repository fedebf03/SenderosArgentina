package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservarRecorridosUseCase @Inject constructor(
    private val recorridoRepositorio: RecorridoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    operator fun invoke(): Result<Flow<List<RecorridoRealizado>>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))
        return Result.success(recorridoRepositorio.observarRecorridos(usuario.id))
    }
}
