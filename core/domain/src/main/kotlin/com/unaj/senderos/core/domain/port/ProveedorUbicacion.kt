package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import kotlinx.coroutines.flow.Flow

interface ProveedorUbicacion {

    // Obtener la ubicación actual; el éxito con null indica sin permiso o sin ubicación disponible
    suspend fun obtenerUbicacionActual(): Result<Coordenada?>

    fun observarUbicaciones(): Flow<LecturaUbicacion>
}
