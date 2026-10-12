package com.unaj.senderos.domain.repository

import com.unaj.senderos.domain.model.CapacidadesDispositivo

interface DetectorCapacidades {

    // Detectar qué funciones tiene el equipo (GPS, acelerómetro, brújula y cámara)
    fun detectar(): CapacidadesDispositivo
}
