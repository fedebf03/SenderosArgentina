package com.unaj.senderos.falsos

import com.unaj.senderos.data.local.CoordenadaEntidad
import com.unaj.senderos.data.local.RecorridoEntidad
import com.unaj.senderos.data.remote.CoordenadaRed
import com.unaj.senderos.data.remote.RecorridoRed
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.RecorridoRealizado

fun crearRecorridoDePrueba(
    id: String = "recorrido-1",
    idUsuario: String = "usuario-1",
    idSendero: String = "sendero-1",
    nombreSendero: String = "Cerro Catedral",
    inicioMilisegundos: Long = 1_000L,
    finMilisegundos: Long = 3_601_000L,
    pasos: Int? = null,
    ritmoPasosPorMinuto: Double? = null,
    esSimulado: Boolean = false,
    estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.PENDIENTE
): RecorridoRealizado {
    return RecorridoRealizado(
        id = id,
        idUsuario = idUsuario,
        idSendero = idSendero,
        nombreSendero = nombreSendero,
        inicioMilisegundos = inicioMilisegundos,
        finMilisegundos = finMilisegundos,
        duracionMilisegundos = finMilisegundos - inicioMilisegundos,
        distanciaKilometros = 5.0,
        velocidadPromedioKilometrosPorHora = 5.0,
        velocidadMaximaKilometrosPorHora = 7.2,
        pasos = pasos,
        ritmoPasosPorMinuto = ritmoPasosPorMinuto,
        trazadoCaminado = listOf(Coordenada(-39.95, -71.07), Coordenada(-39.96, -71.07)),
        esSimulado = esSimulado,
        estadoSincronizacion = estadoSincronizacion
    )
}

fun crearEntidadRecorridoDePrueba(
    id: String = "recorrido-1",
    idUsuario: String = "usuario-1",
    fechaModificacion: Long = 1_000L,
    estadoSincronizacion: String = "PENDIENTE"
): RecorridoEntidad {
    return RecorridoEntidad(
        id = id,
        idUsuario = idUsuario,
        idSendero = "sendero-1",
        nombreSendero = "Cerro Catedral",
        inicioMilisegundos = 1_000L,
        finMilisegundos = 3_601_000L,
        duracionMilisegundos = 3_600_000L,
        distanciaKilometros = 5.0,
        velocidadPromedioKilometrosPorHora = 5.0,
        velocidadMaximaKilometrosPorHora = 7.2,
        pasos = null,
        ritmoPasosPorMinuto = null,
        trazadoCaminado = listOf(CoordenadaEntidad(-39.95, -71.07), CoordenadaEntidad(-39.96, -71.07)),
        esSimulado = false,
        estadoSincronizacion = estadoSincronizacion,
        fechaModificacion = fechaModificacion
    )
}

fun crearRecorridoRedDePrueba(
    id: String = "recorrido-1",
    fechaModificacion: Long = 1_000L
): RecorridoRed {
    return RecorridoRed(
        id = id,
        idSendero = "sendero-1",
        nombreSendero = "Cerro Catedral",
        inicioMilisegundos = 1_000L,
        finMilisegundos = 3_601_000L,
        duracionMilisegundos = 3_600_000L,
        distanciaKilometros = 5.0,
        velocidadPromedioKilometrosPorHora = 5.0,
        velocidadMaximaKilometrosPorHora = 7.2,
        pasos = null,
        ritmoPasosPorMinuto = null,
        trazadoCaminado = listOf(CoordenadaRed(-39.95, -71.07), CoordenadaRed(-39.96, -71.07)),
        esSimulado = false,
        fechaModificacion = fechaModificacion
    )
}
