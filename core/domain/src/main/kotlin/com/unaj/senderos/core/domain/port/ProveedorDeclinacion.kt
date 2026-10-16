package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada

interface ProveedorDeclinacion {

    // Positiva al este; sirve para pasar de norte magnético a verdadero
    fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float
}
