package com.unaj.senderos.domain.port

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import kotlinx.coroutines.flow.Flow

interface ProveedorUbicacion {

    // Obtener la ubicación actual; el éxito con null indica sin permiso o sin ubicación disponible
    suspend fun obtenerUbicacionActual(): Result<Coordenada?>

    // Observar las ubicaciones del dispositivo de forma continua con rumbo y velocidad cuando existen
    fun observarUbicaciones(): Flow<LecturaUbicacion>
}
