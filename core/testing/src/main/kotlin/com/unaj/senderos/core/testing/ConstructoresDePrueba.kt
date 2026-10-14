package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.PerfilElevacion

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
    recorrido: List<PuntoRecorrido> = emptyList(),
    bioma: Bioma = Bioma.SIN_DEFINIR,
    tipoRecorrido: TipoRecorrido = TipoRecorrido.CIRCULAR,
    perfilElevacion: PerfilElevacion? = null
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
        tipoRecorrido = tipoRecorrido,
        puntoInicio = "Base del cerro",
        mejorEpoca = "Verano",
        recomendaciones = listOf("Llevar agua"),
        servicios = listOf("Refugio"),
        etapas = listOf(Etapa(numero = 1, nombre = "Base a cumbre", distanciaKilometros = 6.0)),
        esFavorito = esFavorito,
        estadoSincronizacion = estadoSincronizacion,
        recorrido = recorrido,
        bioma = bioma,
        perfilElevacion = perfilElevacion
    )
}
