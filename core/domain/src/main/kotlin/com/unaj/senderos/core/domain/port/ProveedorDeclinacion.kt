package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.Coordenada

interface ProveedorDeclinacion {

    // Obtener la declinación magnética en grados (positiva al este) en una coordenada e instante, para pasar de norte magnético a verdadero
    fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float
}
