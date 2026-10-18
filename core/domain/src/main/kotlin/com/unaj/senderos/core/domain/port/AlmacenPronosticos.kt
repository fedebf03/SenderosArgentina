package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.clima.PronosticoClima

// Guarda el último pronóstico de cada lugar para mostrarlo sin conexión; es una copia local, no se sincroniza
interface AlmacenPronosticos {
    // Guardar el pronóstico bajo la clave del lugar, reemplazando el anterior
    suspend fun guardar(clave: String, pronostico: PronosticoClima): Result<Unit>

    // Leer el pronóstico guardado de la clave; nulo si no hay o si el archivo está dañado
    suspend fun leer(clave: String): PronosticoClima?
}
