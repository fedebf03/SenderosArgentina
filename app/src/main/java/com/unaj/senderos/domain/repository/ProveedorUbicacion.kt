package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Coordenada

interface ProveedorUbicacion {

    // Obtener la ubicación actual; el éxito con null indica sin permiso o sin ubicación disponible
    suspend fun obtenerUbicacionActual(): Result<Coordenada?>
}
