package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido

fun crearSenderoDePrueba(
    id: String = "sendero-1",
    idUsuario: String = "usuario-1",
    nombre: String = "Cerro Catedral",
    region: String = "Bariloche",
    distanciaKilometros: Double = 12.0,
    dificultad: Dificultad = Dificultad.MEDIA,
    descripcion: String = "Ascenso con vistas al lago",
    desnivelMetros: Int = 600,
    alturaMaximaMetros: Int = 2000,
    alturaMinimaMetros: Int = 1400,
    esFavorito: Boolean = false,
    estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
    recorrido: List<PuntoRecorrido> = emptyList()
): Sendero {
    return Sendero(
        id = id,
        idUsuario = idUsuario,
        nombre = nombre,
        region = region,
        distanciaKilometros = distanciaKilometros,
        dificultad = dificultad,
        descripcion = descripcion,
        desnivelMetros = desnivelMetros,
        duracionEstimada = "5 horas",
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        tipoRecorrido = TipoRecorrido.CIRCULAR,
        puntoInicio = "Base del cerro",
        mejorEpoca = "Verano",
        recomendaciones = listOf("Llevar agua"),
        servicios = listOf("Refugio"),
        etapas = listOf(Etapa(numero = 1, nombre = "Base a cumbre", distanciaKilometros = 6.0)),
        esFavorito = esFavorito,
        estadoSincronizacion = estadoSincronizacion,
        recorrido = recorrido
    )
}
