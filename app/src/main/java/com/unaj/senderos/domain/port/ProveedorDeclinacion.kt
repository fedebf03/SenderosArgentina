package com.unaj.senderos.domain.port

import com.unaj.senderos.domain.model.Coordenada

interface ProveedorDeclinacion {

    // Positiva al este; sirve para pasar de norte magnético a verdadero
    fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float
}
