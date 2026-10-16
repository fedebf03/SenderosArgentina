package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.RecorridoRealizado

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
