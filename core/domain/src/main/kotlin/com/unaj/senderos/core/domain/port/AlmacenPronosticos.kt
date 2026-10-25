package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.clima.PronosticoClima

// Guardamos el último pronóstico de cada lugar para mostrarlo sin conexión. Es una copia local y no se sincroniza
interface AlmacenPronosticos {
    suspend fun guardar(clave: String, pronostico: PronosticoClima): Result<Unit>

    // También devuelve nulo si el archivo está dañado
    suspend fun leer(clave: String): PronosticoClima?
}
