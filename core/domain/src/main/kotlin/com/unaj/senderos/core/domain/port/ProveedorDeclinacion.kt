package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada

interface ProveedorDeclinacion {

    // Positiva al este. Sirve para pasar del norte magnético al verdadero
    fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float
}
