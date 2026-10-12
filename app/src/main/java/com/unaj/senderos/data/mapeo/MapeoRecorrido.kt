package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.CoordenadaEntidad
import com.unaj.senderos.data.local.RecorridoEntidad
import com.unaj.senderos.data.remote.CoordenadaRed
import com.unaj.senderos.data.remote.RecorridoRed
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.RecorridoRealizado

// Resolver el estado de sincronización por nombre; un valor desconocido se toma como sincronizado
private fun resolverEstadoDelRecorrido(nombre: String): EstadoSincronizacion {
    return EstadoSincronizacion.entries.firstOrNull { valor -> valor.name == nombre }
        ?: EstadoSincronizacion.SINCRONIZADO
}

// Convertir la fila local en el modelo de dominio
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

// Convertir el modelo de dominio en fila local con la fecha de modificación y el estado indicados
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

// Convertir el documento remoto en fila local sincronizada del usuario indicado
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
