package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.RecorridoRepositorio
import kotlinx.coroutines.flow.Flow

class ObservarRecorridosUseCase(
    private val recorridoRepositorio: RecorridoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Observar los recorridos del usuario con sesión activa, del más reciente al más viejo
    operator fun invoke(): Result<Flow<List<RecorridoRealizado>>> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))
        return Result.success(recorridoRepositorio.observarRecorridos(usuario.id))
    }
}
