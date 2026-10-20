package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import kotlinx.coroutines.flow.Flow

interface ProveedorUbicacion {

    // Un éxito con nulo indica que no hay permiso o que no hay ubicación disponible
    suspend fun obtenerUbicacionActual(): Result<Coordenada?>

    fun observarUbicaciones(): Flow<LecturaUbicacion>
}
