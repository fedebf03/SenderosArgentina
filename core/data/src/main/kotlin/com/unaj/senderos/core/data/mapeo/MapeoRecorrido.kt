package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.database.CoordenadaEntidad
import com.unaj.senderos.core.database.RecorridoEntidad
import com.unaj.senderos.core.network.CoordenadaRed
import com.unaj.senderos.core.network.RecorridoRed
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.RecorridoRealizado

// Un valor desconocido se toma como sincronizado
private fun resolverEstadoDelRecorrido(nombre: String): EstadoSincronizacion {
    return EstadoSincronizacion.entries.firstOrNull { valor -> valor.name == nombre }
        ?: EstadoSincronizacion.SINCRONIZADO
}

fun RecorridoEntidad.aDominio(): RecorridoRealizado {
    return RecorridoRealizado(
        id = id,
        idUsuario = idUsuario,
        idSendero = idSendero,
        nombreSendero = nombreSendero,
        inicioMilisegundos = inicioMilisegundos,
        finMilisegundos = finMilisegundos,
        duracionMilisegundos = duracionMilisegundos,
        distanciaKilometros = distanciaKilometros,
        velocidadPromedioKilometrosPorHora = velocidadPromedioKilometrosPorHora,
        velocidadMaximaKilometrosPorHora = velocidadMaximaKilometrosPorHora,
        pasos = pasos,
        ritmoPasosPorMinuto = ritmoPasosPorMinuto,
        trazadoCaminado = trazadoCaminado.map { posicion -> Coordenada(posicion.latitud, posicion.longitud) },
        esSimulado = esSimulado,
        estadoSincronizacion = resolverEstadoDelRecorrido(estadoSincronizacion)
    )
}

fun RecorridoRealizado.aEntidad(
    fechaModificacion: Long,
    estadoSincronizacion: EstadoSincronizacion
): RecorridoEntidad {
    return RecorridoEntidad(
        id = id,
        idUsuario = idUsuario,
        idSendero = idSendero,
        nombreSendero = nombreSendero,
        inicioMilisegundos = inicioMilisegundos,
        finMilisegundos = finMilisegundos,
        duracionMilisegundos = duracionMilisegundos,
        distanciaKilometros = distanciaKilometros,
        velocidadPromedioKilometrosPorHora = velocidadPromedioKilometrosPorHora,
        velocidadMaximaKilometrosPorHora = velocidadMaximaKilometrosPorHora,
        pasos = pasos,
        ritmoPasosPorMinuto = ritmoPasosPorMinuto,
        trazadoCaminado = trazadoCaminado.map { posicion -> CoordenadaEntidad(posicion.latitud, posicion.longitud) },
        esSimulado = esSimulado,
        estadoSincronizacion = estadoSincronizacion.name,
        fechaModificacion = fechaModificacion
    )
}

// Convertir la fila local en el documento remoto, que no lleva idUsuario ni estado de sincronización
fun RecorridoEntidad.aRed(): RecorridoRed {
    return RecorridoRed(
        id = id,
        idSendero = idSendero,
        nombreSendero = nombreSendero,
        inicioMilisegundos = inicioMilisegundos,
        finMilisegundos = finMilisegundos,
        duracionMilisegundos = duracionMilisegundos,
        distanciaKilometros = distanciaKilometros,
        velocidadPromedioKilometrosPorHora = velocidadPromedioKilometrosPorHora,
        velocidadMaximaKilometrosPorHora = velocidadMaximaKilometrosPorHora,
        pasos = pasos,
        ritmoPasosPorMinuto = ritmoPasosPorMinuto,
        trazadoCaminado = trazadoCaminado.map { posicion -> CoordenadaRed(posicion.latitud, posicion.longitud) },
        esSimulado = esSimulado,
        fechaModificacion = fechaModificacion
    )
}

fun RecorridoRed.aEntidad(idUsuario: String): RecorridoEntidad {
    return RecorridoEntidad(
        id = id,
        idUsuario = idUsuario,
        idSendero = idSendero,
        nombreSendero = nombreSendero,
        inicioMilisegundos = inicioMilisegundos,
        finMilisegundos = finMilisegundos,
        duracionMilisegundos = duracionMilisegundos,
        distanciaKilometros = distanciaKilometros,
        velocidadPromedioKilometrosPorHora = velocidadPromedioKilometrosPorHora,
        velocidadMaximaKilometrosPorHora = velocidadMaximaKilometrosPorHora,
        pasos = pasos,
        ritmoPasosPorMinuto = ritmoPasosPorMinuto,
        trazadoCaminado = trazadoCaminado.map { posicion -> CoordenadaEntidad(posicion.latitud, posicion.longitud) },
        esSimulado = esSimulado,
        estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO.name,
        fechaModificacion = fechaModificacion
    )
}
